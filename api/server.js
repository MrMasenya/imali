require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const multer = require('multer');
const { OAuth2Client } = require('google-auth-library');

const app = express();
app.use(cors());
app.use(express.json({ limit: '2mb' }));

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const googleClient = process.env.GOOGLE_CLIENT_ID
  ? new OAuth2Client(process.env.GOOGLE_CLIENT_ID)
  : null;

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 5 * 1024 * 1024 } });

let firebaseAdmin = null;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
  try {
    const admin = require('firebase-admin');
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
    });
    firebaseAdmin = admin;
    console.log('Firebase Admin initialised for FCM');
  } catch (e) {
    console.warn('Firebase Admin not loaded:', e.message);
  }
}

async function sendPush(fcmToken, title, body) {
  if (!firebaseAdmin || !fcmToken) return;
  try {
    await firebaseAdmin.messaging().send({
      token: fcmToken,
      notification: { title, body },
    });
    console.log('FCM sent:', title);
  } catch (e) {
    console.warn('FCM failed:', e.message);
  }
}

const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true, lowercase: true },
  passwordHash: { type: String, default: '' },
  googleId: { type: String, sparse: true },
  currency: { type: String, default: 'ZAR' },
  theme: { type: String, default: 'system' },
  language: { type: String, default: 'en' },
  notify: { type: Boolean, default: true },
  fcmToken: { type: String, default: '' },
  streakDays: { type: Number, default: 0 },
  lastCaptureDate: { type: String, default: '' },
  badges: { type: [String], default: [] },
  createdAt: { type: Date, default: Date.now },
});

const transactionSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  amount: { type: Number, required: true },
  type: { type: String, enum: ['income', 'expense'], required: true },
  category: { type: String, required: true },
  note: { type: String, default: '' },
  date: { type: String, required: true },
  photoUrl: { type: String, default: '' },
  createdAt: { type: Date, default: Date.now },
});

const budgetSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  category: { type: String, required: true },
  month: { type: String, required: true },
  limit: { type: Number, required: true },
});

const stokvelSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  name: { type: String, required: true },
  members: { type: [String], default: [] },
  contributions: {
    type: [{ member: String, amount: Number, date: String }],
    default: [],
  },
  createdAt: { type: Date, default: Date.now },
});

const blobSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  filename: String,
  contentType: String,
  data: { type: Buffer, required: true },
  createdAt: { type: Date, default: Date.now },
});

const User = mongoose.model('User', userSchema);
const Transaction = mongoose.model('Transaction', transactionSchema);
const Budget = mongoose.model('Budget', budgetSchema);
const Stokvel = mongoose.model('Stokvel', stokvelSchema);
const BlobStore = mongoose.model('BlobStore', blobSchema);

function auth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ message: 'Missing token' });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    return res.status(401).json({ message: 'Invalid token' });
  }
}

function signToken(user) {
  return jwt.sign({ id: user._id.toString(), email: user.email }, JWT_SECRET, { expiresIn: '14d' });
}

function sanitizeUser(user) {
  return {
    _id: user._id.toString(),
    name: user.name,
    email: user.email,
    currency: user.currency,
    theme: user.theme,
    language: user.language,
    notify: user.notify,
    streakDays: user.streakDays,
    badges: user.badges,
  };
}

async function updateGamification(user) {
  const today = new Date().toISOString().slice(0, 10);
  if (user.lastCaptureDate === today) return user;
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
  if (user.lastCaptureDate === yesterday) user.streakDays += 1;
  else user.streakDays = 1;
  user.lastCaptureDate = today;
  if (user.streakDays >= 7 && !user.badges.includes('week_streak')) {
    user.badges.push('week_streak');
  }
  await user.save();
  return user;
}

async function checkBudgetAlert(userId, category, month) {
  const user = await User.findById(userId);
  if (!user?.notify || !user.fcmToken) return;
  const budget = await Budget.findOne({ userId, category, month });
  if (!budget) return;
  const txs = await Transaction.find({ userId, category, type: 'expense', date: new RegExp(`^${month}`) });
  const spent = txs.reduce((s, t) => s + t.amount, 0);
  const ratio = spent / budget.limit;
  if (ratio >= 0.8 && ratio < 0.85) {
    await sendPush(user.fcmToken, 'iMali budget alert', `${category} is at ${Math.round(ratio * 100)}% of your limit`);
  }
}

app.get('/', (_req, res) => res.json({ message: 'iMali API running', version: '2.0.0' }));

app.post('/api/auth/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (!name?.trim() || !email?.trim() || !password) {
      return res.status(400).json({ message: 'Name, email and password are required' });
    }
    if (password.length < 6) return res.status(400).json({ message: 'Password must be at least 6 characters' });
    const existing = await User.findOne({ email: email.toLowerCase() });
    if (existing) return res.status(409).json({ message: 'Email already registered' });
    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({ name: name.trim(), email: email.toLowerCase(), passwordHash });
    const token = signToken(user);
    console.log(`Registered: ${user.email}`);
    res.status(201).json({ token, user: sanitizeUser(user) });
  } catch (err) {
    console.error('Register error:', err.message);
    res.status(500).json({ message: 'Registration failed' });
  }
});

app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email?.trim() || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }
    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user || !user.passwordHash) return res.status(401).json({ message: 'Invalid credentials' });
    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ message: 'Invalid credentials' });
    const token = signToken(user);
    console.log(`Login: ${user.email}`);
    res.json({ token, user: sanitizeUser(user) });
  } catch (err) {
    console.error('Login error:', err.message);
    res.status(500).json({ message: 'Login failed' });
  }
});

app.post('/api/auth/sso', async (req, res) => {
  try {
    const { idToken } = req.body;
    if (!idToken) return res.status(400).json({ message: 'idToken required' });
    if (!googleClient) return res.status(503).json({ message: 'Google SSO not configured on server' });
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    const email = payload.email.toLowerCase();
    let user = await User.findOne({ $or: [{ email }, { googleId: payload.sub }] });
    if (!user) {
      user = await User.create({
        name: payload.name || email.split('@')[0],
        email,
        googleId: payload.sub,
        passwordHash: '',
      });
      console.log(`Google SSO new user: ${email}`);
    } else if (!user.googleId) {
      user.googleId = payload.sub;
      await user.save();
    }
    const token = signToken(user);
    res.json({ token, user: sanitizeUser(user) });
  } catch (err) {
    console.error('SSO error:', err.message);
    res.status(401).json({ message: 'Google sign-in failed' });
  }
});

app.get('/api/users/me', auth, async (req, res) => {
  const user = await User.findById(req.user.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  res.json(sanitizeUser(user));
});

app.put('/api/users/me', auth, async (req, res) => {
  try {
    const { name, currency, theme, language, notify } = req.body;
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ message: 'User not found' });
    if (name != null) user.name = name.trim();
    if (currency != null) user.currency = currency;
    if (theme != null) user.theme = theme;
    if (language != null) user.language = language;
    if (notify != null) user.notify = notify;
    await user.save();
    res.json(sanitizeUser(user));
  } catch (err) {
    res.status(500).json({ message: 'Could not update settings' });
  }
});

app.put('/api/users/fcm-token', auth, async (req, res) => {
  const user = await User.findById(req.user.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  user.fcmToken = req.body.token || '';
  await user.save();
  console.log(`FCM token saved for ${user.email}`);
  res.json({ message: 'Token saved' });
});

app.get('/api/transactions', auth, async (req, res) => {
  const items = await Transaction.find({ userId: req.user.id }).sort({ createdAt: -1 });
  res.json(items.map((t) => ({ ...t.toObject(), _id: t._id.toString() })));
});

app.post('/api/transactions', auth, async (req, res) => {
  try {
    const body = Array.isArray(req.body) ? req.body : [req.body];
    const saved = [];
    for (const item of body) {
      const { amount, type, category, note, date, photoUrl } = item;
      if (amount == null || !type || !category || !date) {
        return res.status(400).json({ message: 'amount, type, category and date are required' });
      }
      const tx = await Transaction.create({
        userId: req.user.id,
        amount: Number(amount),
        type,
        category,
        note: note || '',
        date,
        photoUrl: photoUrl || '',
      });
      saved.push({ ...tx.toObject(), _id: tx._id.toString() });
      const month = date.slice(0, 7);
      if (type === 'expense') await checkBudgetAlert(req.user.id, category, month);
    }
    const user = await User.findById(req.user.id);
    if (user) await updateGamification(user);
    res.status(201).json(Array.isArray(req.body) ? saved : saved[0]);
  } catch (err) {
    console.error('Add transaction error:', err.message);
    res.status(500).json({ message: 'Could not add transaction' });
  }
});

app.delete('/api/transactions/:id', auth, async (req, res) => {
  const deleted = await Transaction.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
  if (!deleted) return res.status(404).json({ message: 'Transaction not found' });
  res.json({ message: 'Deleted' });
});

app.get('/api/budgets', auth, async (req, res) => {
  const items = await Budget.find({ userId: req.user.id }).sort({ month: -1 });
  res.json(items.map((b) => ({ ...b.toObject(), _id: b._id.toString() })));
});

app.post('/api/budgets', auth, async (req, res) => {
  try {
    const { category, month, limit } = req.body;
    if (!category || !month || limit == null) {
      return res.status(400).json({ message: 'category, month and limit are required' });
    }
    const budget = await Budget.create({
      userId: req.user.id,
      category,
      month,
      limit: Number(limit),
    });
    res.status(201).json({ ...budget.toObject(), _id: budget._id.toString() });
  } catch (err) {
    res.status(500).json({ message: 'Could not add budget' });
  }
});

app.get('/api/stokvels', auth, async (req, res) => {
  const items = await Stokvel.find({ userId: req.user.id }).sort({ createdAt: -1 });
  res.json(items.map((s) => ({ ...s.toObject(), _id: s._id.toString() })));
});

app.post('/api/stokvels', auth, async (req, res) => {
  try {
    const { name, members } = req.body;
    if (!name?.trim()) return res.status(400).json({ message: 'Name required' });
    const stokvel = await Stokvel.create({
      userId: req.user.id,
      name: name.trim(),
      members: members || [],
    });
    res.status(201).json({ ...stokvel.toObject(), _id: stokvel._id.toString() });
  } catch (err) {
    res.status(500).json({ message: 'Could not create stokvel' });
  }
});

app.post('/api/stokvels/:id/contribute', auth, async (req, res) => {
  const stokvel = await Stokvel.findOne({ _id: req.params.id, userId: req.user.id });
  if (!stokvel) return res.status(404).json({ message: 'Stokvel not found' });
  const { member, amount, date } = req.body;
  if (!member || amount == null) return res.status(400).json({ message: 'member and amount required' });
  stokvel.contributions.push({
    member,
    amount: Number(amount),
    date: date || new Date().toISOString().slice(0, 10),
  });
  await stokvel.save();
  const user = await User.findById(req.user.id);
  if (user?.fcmToken) {
    await sendPush(user.fcmToken, 'Stokvel contribution', `${member} contributed R${amount}`);
  }
  res.json({ ...stokvel.toObject(), _id: stokvel._id.toString() });
});

app.post('/api/blobs', auth, upload.single('file'), async (req, res) => {
  if (!req.file) return res.status(400).json({ message: 'file required' });
  const blob = await BlobStore.create({
    userId: req.user.id,
    filename: req.file.originalname,
    contentType: req.file.mimetype,
    data: req.file.buffer,
  });
  const url = `/api/blobs/${blob._id}`;
  console.log(`Blob stored: ${blob._id} (${req.file.size} bytes)`);
  res.status(201).json({ id: blob._id.toString(), url, contentType: blob.contentType });
});

app.get('/api/blobs/:id', auth, async (req, res) => {
  const blob = await BlobStore.findOne({ _id: req.params.id, userId: req.user.id });
  if (!blob) return res.status(404).json({ message: 'Not found' });
  res.set('Content-Type', blob.contentType || 'application/octet-stream');
  res.send(blob.data);
});

const PORT = process.env.PORT || 4000;
mongoose
  .connect(process.env.MONGO_URI)
  .then(() => app.listen(PORT, () => console.log(`iMali API listening on port ${PORT}`)))
  .catch((e) => {
    console.error('DB connection failed:', e.message);
    process.exit(1);
  });
