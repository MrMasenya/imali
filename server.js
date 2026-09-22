require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');

const app = express();
app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';

// --- Schemas ---
const userSchema = new mongoose.Schema({
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true, lowercase: true },
  passwordHash: { type: String, required: true },
  currency: { type: String, default: 'ZAR' },
  theme: { type: String, default: 'system' },
  notify: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now },
});

const transactionSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  amount: { type: Number, required: true },
  type: { type: String, enum: ['income', 'expense'], required: true },
  category: { type: String, required: true },
  note: { type: String, default: '' },
  date: { type: String, required: true },
  createdAt: { type: Date, default: Date.now },
});

const budgetSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  category: { type: String, required: true },
  month: { type: String, required: true },
  limit: { type: Number, required: true },
});

const User = mongoose.model('User', userSchema);
const Transaction = mongoose.model('Transaction', transactionSchema);
const Budget = mongoose.model('Budget', budgetSchema);

// --- Auth middleware ---
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
  return jwt.sign({ id: user._id.toString(), email: user.email }, JWT_SECRET, { expiresIn: '7d' });
}

function sanitizeUser(user) {
  return {
    _id: user._id.toString(),
    name: user.name,
    email: user.email,
    currency: user.currency,
    theme: user.theme,
    notify: user.notify,
  };
}

// --- Routes ---
app.get('/', (_req, res) => res.json({ message: 'iMali API running' }));

app.post('/api/auth/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (!name?.trim() || !email?.trim() || !password) {
      return res.status(400).json({ message: 'Name, email and password are required' });
    }
    if (password.length < 6) {
      return res.status(400).json({ message: 'Password must be at least 6 characters' });
    }
    const existing = await User.findOne({ email: email.toLowerCase() });
    if (existing) return res.status(409).json({ message: 'Email already registered' });

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({ name: name.trim(), email: email.toLowerCase(), passwordHash });
    const token = signToken(user);
    console.log(`Registered user: ${user.email}`);
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
    if (!user) return res.status(401).json({ message: 'Invalid credentials' });

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

app.get('/api/users/me', auth, async (req, res) => {
  const user = await User.findById(req.user.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  res.json(sanitizeUser(user));
});

app.put('/api/users/me', auth, async (req, res) => {
  try {
    const { name, currency, theme } = req.body;
    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ message: 'User not found' });

    if (name != null) user.name = name.trim();
    if (currency != null) user.currency = currency;
    if (theme != null) user.theme = theme;
    await user.save();
    console.log(`Settings updated: ${user.email}`);
    res.json(sanitizeUser(user));
  } catch (err) {
    console.error('Settings error:', err.message);
    res.status(500).json({ message: 'Could not update settings' });
  }
});

app.get('/api/transactions', auth, async (req, res) => {
  const items = await Transaction.find({ userId: req.user.id }).sort({ createdAt: -1 });
  res.json(items.map((t) => ({ ...t.toObject(), _id: t._id.toString() })));
});

app.post('/api/transactions', auth, async (req, res) => {
  try {
    const { amount, type, category, note, date } = req.body;
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
    });
    console.log(`Transaction added for user ${req.user.id}: ${type} ${amount}`);
    res.status(201).json({ ...tx.toObject(), _id: tx._id.toString() });
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
    console.error('Add budget error:', err.message);
    res.status(500).json({ message: 'Could not add budget' });
  }
});

const PORT = process.env.PORT || 4000;
mongoose
  .connect(process.env.MONGO_URI)
  .then(() => app.listen(PORT, () => console.log(`iMali API listening on port ${PORT}`)))
  .catch((e) => {
    console.error('DB connection failed:', e.message);
    process.exit(1);
  });
