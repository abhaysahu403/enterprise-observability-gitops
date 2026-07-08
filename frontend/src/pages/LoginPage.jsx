import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Paper, TextField, Button, Typography, Alert, InputAdornment, IconButton, Stack
} from '@mui/material';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import VisibilityOffRoundedIcon from '@mui/icons-material/VisibilityOffRounded';
import HubRoundedIcon from '@mui/icons-material/HubRounded';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/client';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(username, password);
      const redirectTo = location.state?.from?.pathname || '/dashboard';
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{
      minHeight: '100vh', display: 'grid', placeItems: 'center',
      background: 'radial-gradient(circle at 20% 20%, rgba(31,182,201,0.12), transparent 45%), #12161C'
    }}>
      <Paper elevation={0} sx={{ width: 400, p: 4, border: '1px solid rgba(231,236,239,0.08)' }}>
        <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
          <Box sx={{
            width: 48, height: 48, borderRadius: 2, display: 'grid', placeItems: 'center',
            background: 'linear-gradient(135deg, #1FB6C9 0%, #0E8C9C 100%)'
          }}>
            <HubRoundedIcon sx={{ color: '#04181A' }} />
          </Box>
          <Typography variant="h5" fontWeight={800}>Nimbus</Typography>
          <Typography variant="body2" color="text.secondary">Enterprise Operations Platform</Typography>
        </Stack>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <form onSubmit={handleSubmit}>
          <Stack spacing={2}>
            <TextField
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              fullWidth
              autoFocus
              required
            />
            <TextField
              label="Password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              fullWidth
              required
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPassword((s) => !s)} edge="end" size="small">
                      {showPassword ? <VisibilityOffRoundedIcon fontSize="small" /> : <VisibilityRoundedIcon fontSize="small" />}
                    </IconButton>
                  </InputAdornment>
                )
              }}
            />
            <Button type="submit" variant="contained" size="large" disabled={submitting} fullWidth>
              {submitting ? 'Signing in…' : 'Sign in'}
            </Button>
          </Stack>
        </form>

        <Box sx={{ mt: 3, p: 1.5, borderRadius: 2, bgcolor: 'action.hover' }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'IBM Plex Mono, monospace', display: 'block' }}>
            demo accounts &middot; password: Password@123
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'IBM Plex Mono, monospace', display: 'block' }}>
            admin · hr.sharma · mgr.rao · emp.verma
          </Typography>
        </Box>
      </Paper>
    </Box>
  );
}
