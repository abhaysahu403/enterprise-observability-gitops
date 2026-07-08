import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  Box, Drawer, AppBar, Toolbar, List, ListItemButton, ListItemIcon, ListItemText,
  Typography, IconButton, Avatar, Menu, MenuItem, Divider, Chip, useMediaQuery, Tooltip
} from '@mui/material';
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded';
import PeopleAltRoundedIcon from '@mui/icons-material/PeopleAltRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import PaidRoundedIcon from '@mui/icons-material/PaidRounded';
import LaptopMacRoundedIcon from '@mui/icons-material/LaptopMacRounded';
import ConfirmationNumberRoundedIcon from '@mui/icons-material/ConfirmationNumberRounded';
import NotificationsRoundedIcon from '@mui/icons-material/NotificationsRounded';
import BarChartRoundedIcon from '@mui/icons-material/BarChartRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import HubRoundedIcon from '@mui/icons-material/HubRounded';
import { useAuth } from '../context/AuthContext';
import { useColorMode } from '../context/ColorModeContext';

const DRAWER_WIDTH = 248;

const NAV_ITEMS = [
  { label: 'Dashboard', to: '/dashboard', icon: DashboardRoundedIcon },
  { label: 'Employees', to: '/employees', icon: PeopleAltRoundedIcon },
  { label: 'Leave', to: '/leaves', icon: EventAvailableRoundedIcon },
  { label: 'Payroll', to: '/payroll', icon: PaidRoundedIcon },
  { label: 'Assets', to: '/assets', icon: LaptopMacRoundedIcon },
  { label: 'Tickets', to: '/tickets', icon: ConfirmationNumberRoundedIcon },
  { label: 'Notifications', to: '/notifications', icon: NotificationsRoundedIcon },
  { label: 'Reports', to: '/reports', icon: BarChartRoundedIcon },
  { label: 'Admin', to: '/admin', icon: AdminPanelSettingsRoundedIcon, roles: ['ADMIN'] },
  { label: 'Settings', to: '/settings', icon: SettingsRoundedIcon }
];

export default function Layout() {
  const { user, hasRole, logout } = useAuth();
  const { mode, toggleColorMode } = useColorMode();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleLogout = async () => {
    setAnchorEl(null);
    await logout();
    navigate('/login');
  };

  const initials = user ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase() : '?';

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' }
        }}
      >
        <Toolbar sx={{ gap: 1, py: 2 }}>
          <Box sx={{
            width: 34, height: 34, borderRadius: 1.5, display: 'grid', placeItems: 'center',
            background: 'linear-gradient(135deg, #1FB6C9 0%, #0E8C9C 100%)'
          }}>
            <HubRoundedIcon sx={{ color: '#04181A', fontSize: 20 }} />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={800} lineHeight={1.1}>Nimbus</Typography>
            <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>
              ops-platform
            </Typography>
          </Box>
        </Toolbar>
        <Divider />
        <List sx={{ px: 1.5, py: 1.5, flexGrow: 1 }}>
          {NAV_ITEMS.filter((item) => !item.roles || hasRole(...item.roles)).map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                '&.active': {
                  bgcolor: 'action.selected',
                  '& .MuiListItemIcon-root': { color: 'primary.main' },
                  '& .MuiListItemText-primary': { fontWeight: 700 }
                }
              }}
            >
              <ListItemIcon sx={{ minWidth: 38 }}>
                <item.icon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: '0.9rem' }} />
            </ListItemButton>
          ))}
        </List>
        <Divider />
        <Box sx={{ p: 2 }}>
          <Chip
            size="small"
            label="v1.0.0 · demo"
            sx={{ fontSize: '0.65rem', bgcolor: 'action.hover' }}
          />
        </Box>
      </Drawer>

      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
        <AppBar position="sticky" elevation={0}>
          <Toolbar sx={{ justifyContent: 'space-between' }}>
            <Typography variant="body2" color="text.secondary" sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>
              {new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </Typography>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
                <IconButton onClick={toggleColorMode} size="small">
                  {mode === 'dark' ? <LightModeRoundedIcon fontSize="small" /> : <DarkModeRoundedIcon fontSize="small" />}
                </IconButton>
              </Tooltip>

              <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} size="small" sx={{ ml: 1 }}>
                <Avatar sx={{ width: 32, height: 32, fontSize: '0.8rem', bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                  {initials}
                </Avatar>
              </IconButton>
              <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
                <Box sx={{ px: 2, py: 1 }}>
                  <Typography variant="body2" fontWeight={700}>{user?.firstName} {user?.lastName}</Typography>
                  <Typography variant="caption" color="text.secondary">{user?.roles?.join(', ')}</Typography>
                </Box>
                <Divider />
                <MenuItem onClick={() => { setAnchorEl(null); navigate('/settings'); }}>
                  <SettingsRoundedIcon fontSize="small" sx={{ mr: 1.5 }} /> Settings
                </MenuItem>
                <MenuItem onClick={handleLogout}>
                  <LogoutRoundedIcon fontSize="small" sx={{ mr: 1.5 }} /> Log out
                </MenuItem>
              </Menu>
            </Box>
          </Toolbar>
        </AppBar>

        <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, bgcolor: 'background.default' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
