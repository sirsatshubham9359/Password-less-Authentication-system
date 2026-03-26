import { useState, useEffect } from 'react';
import axios from 'axios';
import { io } from 'socket.io-client';
import { QRCodeSVG } from 'qrcode.react';
import './index.css'; 

const socket = io('http://localhost:5000');

function App() {
  const [sessionId, setSessionId] = useState('');
  const [qrData, setQrData] = useState('');
  const [userData, setUserData] = useState(null);
  const [status, setStatus] = useState('idle'); 
  const [timeLeft, setTimeLeft] = useState(60);

  const generateQR = async () => {
    try {
      setStatus('active');
      setTimeLeft(60); 
      setQrData(''); 

      const timestamp = new Date().getTime();
 const response = await axios.get(`http://localhost:5000/api/generate-session?t=${timestamp}`);
      
      setSessionId(response.data.sessionId);
      setQrData(response.data.qrData);
    } catch (error) {
      console.error("Error generating session", error);
    }
  };

  const cancelLogin = () => {
    setStatus('idle');
    setSessionId('');
    setQrData('');
  };

  useEffect(() => {
    let timer;
    if (status === 'active' && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prevTime) => prevTime - 1);
      }, 1000);
    } else if (status === 'active' && timeLeft === 0) {
      setStatus('expired');
      setSessionId(''); 
    }
    return () => clearInterval(timer);
  }, [status, timeLeft]);

  // THIS IS THE MAGIC CONNECTION BLOCK
  useEffect(() => {
    if (sessionId && status === 'active') {
      socket.on(`auth-success-${sessionId}`, (data) => {
        setStatus('authenticated');
        // Grab the Roll Number sent from the Android app
        setUserData(data.rollNo); 

        // REDIRECT TO PERSONALIZED PORTAL AFTER 3 SECONDS
        setTimeout(() => {
          // CHANGE THIS URL to your actual campus portal dashboard!
          // It dynamically attaches the student's Roll Number to the web address
       window.location.href = `/dashboard.html?student=${data.rollNo}&name=${encodeURIComponent(data.name || '')}`;
        }, 3000);

      });
    }
    return () => {
      if (sessionId) socket.off(`auth-success-${sessionId}`);
    };
  }, [sessionId, status]);

  // View: Authenticated Success Screen
  if (status === 'authenticated') {
    return (
      <div className="container">
        <div className="registrationblock">
          <div className="intro">
            <h1>Campus Portal</h1>
          </div>
          <div className="registrationmain">
            <h2>Access Granted!</h2>
            <div className="qr-wrapper" style={{ border: '2px solid green' }}>
              <p style={{ fontSize: '24px', fontWeight: 'bold', color: '#5a3e1b' }}>
                Welcome, Student {userData}
              </p>
              <p style={{ color: '#666', marginTop: '10px', textAlign: 'center' }}>
                You have successfully logged in<br/>without a password.
              </p>
              <p style={{ color: '#28a745', fontWeight: 'bold', marginTop: '15px' }}>
                Redirecting to your dashboard...
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // View: Main Login Flow
  return (
    <div className="container">
      <div className="registrationblock">
        <div className="intro">
          <h1>Campus Portal</h1>
        </div>
        <div className="registrationmain">
          <h2>Secure Login</h2>
          
          {status === 'idle' && (
            <button onClick={generateQR}>Login</button>
          )}

          {status === 'active' && (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              
              <div className="qr-wrapper">
                {qrData ? (
                  <QRCodeSVG value={qrData} size={180} />
                ) : (
                  <p style={{ height: '180px', display: 'flex', alignItems: 'center' }}>
                    Loading...
                  </p>
                )}
              </div>
              
              <p style={{ marginTop: '5px', color: '#5a3e1b', fontWeight: '600', fontSize: '18px' }}>
                Scan with mobile app
              </p>
              <p style={{ color: '#d9534f', fontWeight: 'bold', margin: '5px 0 20px 0', fontSize: '18px' }}>
                Expires in: {timeLeft}s
              </p>
              <button 
                onClick={cancelLogin} 
                style={{ backgroundColor: '#dc3545', width: '200px', height: '45px', fontSize: '16px' }}
              >
                Cancel
              </button>
            </div>
          )}

          {status === 'expired' && (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: '100%' }}>
              
              <div style={{ 
                backgroundColor: 'rgba(255, 255, 255, 0.65)', 
                backdropFilter: 'blur(5px)', 
                border: '1px solid rgba(220, 53, 69, 0.3)', 
                boxShadow: '0 8px 25px rgba(220, 53, 69, 0.15)', 
                padding: '20px 25px', 
                borderRadius: '12px', 
                marginBottom: '30px',
                textAlign: 'center',
                width: '100%',
                maxWidth: '320px'
              }}>
                <p style={{ margin: '0 0 8px 0', color: '#c9302c', fontWeight: 'bold', fontSize: '22px', letterSpacing: '0.5px' }}>
                  ⏳ Session Expired
                </p>
                <p style={{ margin: 0, color: '#5a3e1b', fontSize: '15px', fontWeight: '500', lineHeight: '1.4' }}>
                  For your security, login codes are time-limited. Please generate a new one to continue.
                </p>
              </div>

              <button onClick={generateQR}>Generate Again</button>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}

export default App;