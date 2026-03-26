const express = require('express');
const http = require('http');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const { Server } = require('socket.io');

const app = express();
app.use(cors());
app.use(express.json());
app.use((req, res, next) => { res.setHeader('ngrok-skip-browser-warning', 'true'); next(); });

const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: "*", methods: ["GET", "POST"] }
});

// In-memory store for active login sessions
const activeSessions = {};

// 1. Endpoint to generate a new QR session
app.get('/api/generate-session', (req, res) => {
  const sessionId = uuidv4();
  activeSessions[sessionId] = { status: 'pending', createdAt: Date.now() };
  
  // The QR code will contain this session ID
  res.json({ sessionId, qrData: `campus-auth://login?session=${sessionId}` });
});

// 2. Endpoint for the Mobile App to verify the scan
app.post('/api/verify-scan', (req, res) => {
  // CHANGED userId to rollNo to match Android App!
  const { sessionId, deviceId, rollNo, currentIp } = req.body; 

  if (activeSessions[sessionId] && activeSessions[sessionId].status === 'pending') {
    activeSessions[sessionId].status = 'approved';
    
    // Send the rollNo back to the React website
    io.emit(`auth-success-${sessionId}`, { message: 'Login Granted', rollNo });
    
    res.json({ success: true, message: 'Authorized access granted instantly' });
  } else {
    res.status(400).json({ success: false, message: 'Invalid or expired session' });
  }
});

const PORT = 5000;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));