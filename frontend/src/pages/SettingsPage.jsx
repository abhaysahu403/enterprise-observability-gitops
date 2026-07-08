import { Box, Card, CardContent, Typography, Stack, Switch, FormControlLabel, Chip, Divider } from '@mui/material';
import PageHeader from '../components/PageHeader';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../context/ColorModeContext';

export default function SettingsPage() {
  const { user } = useAuth();
  const { mode, toggleColorMode } = useColorMode();

  return (
    <Box>
      <PageHeader title="Settings" subtitle="Your profile and platform preferences." />

      <Card sx={{ mb: 3, maxWidth: 520 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>Profile</Typography>
          <Stack spacing={1.5}>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">Name</Typography>
              <Typography variant="body2" fontWeight={600}>{user?.firstName} {user?.lastName}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">Username</Typography>
              <Typography variant="body2" fontWeight={600} sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{user?.username}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2" color="text.secondary">Email</Typography>
              <Typography variant="body2" fontWeight={600}>{user?.email}</Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="body2" color="text.secondary">Roles</Typography>
              <Stack direction="row" spacing={0.5}>
                {user?.roles?.map((r) => <Chip key={r} label={r} size="small" />)}
              </Stack>
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      <Card sx={{ maxWidth: 520 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>Appearance</Typography>
          <FormControlLabel
            control={<Switch checked={mode === 'dark'} onChange={toggleColorMode} />}
            label="Dark mode"
          />
          <Divider sx={{ my: 2 }} />
          <Typography variant="caption" color="text.secondary">
            More preferences (notification channels, default landing page) can be added here in a future iteration.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}
