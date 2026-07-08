import { useEffect, useState } from 'react';
import { Grid, Card, CardContent, Typography, Box, Stack, Skeleton, Alert } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';
import PeopleAltRoundedIcon from '@mui/icons-material/PeopleAltRounded';
import ConfirmationNumberRoundedIcon from '@mui/icons-material/ConfirmationNumberRounded';
import LaptopMacRoundedIcon from '@mui/icons-material/LaptopMacRounded';
import NotificationsRoundedIcon from '@mui/icons-material/NotificationsRounded';
import PageHeader from '../components/PageHeader';
import { EmployeeAPI, TicketAPI, AssetAPI, NotificationAPI } from '../api/endpoints';
import { useAuth } from '../context/AuthContext';

const CHART_COLORS = ['#1FB6C9', '#F2A93B', '#2FB673', '#E4572E', '#8B7FD6', '#5B6470'];

function StatCard({ icon: Icon, label, value, tone = 'primary.main' }) {
  return (
    <Card>
      <CardContent>
        <Stack direction="row" alignItems="center" spacing={2}>
          <Box sx={{
            width: 44, height: 44, borderRadius: 2, display: 'grid', placeItems: 'center',
            bgcolor: 'action.hover', color: tone
          }}>
            <Icon />
          </Box>
          <Box>
            <Typography variant="h5" fontWeight={800} sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{value}</Typography>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeEmployees, setActiveEmployees] = useState(0);
  const [deptSummary, setDeptSummary] = useState([]);
  const [ticketSummary, setTicketSummary] = useState({});
  const [assetAvailable, setAssetAvailable] = useState(0);
  const [assetAssigned, setAssetAssigned] = useState(0);
  const [notificationSummary, setNotificationSummary] = useState({});

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const [activeRes, deptRes, ticketRes, availRes, assignedRes, notifRes] = await Promise.allSettled([
          EmployeeAPI.activeCount(),
          EmployeeAPI.departmentSummary(),
          TicketAPI.statusSummary(),
          AssetAPI.countByStatus('AVAILABLE'),
          AssetAPI.countByStatus('ASSIGNED'),
          NotificationAPI.statusSummary()
        ]);
        if (!mounted) return;
        if (activeRes.status === 'fulfilled' && activeRes.value?.data?.data != null) setActiveEmployees(activeRes.value.data.data);
        if (deptRes.status === 'fulfilled' && Array.isArray(deptRes.value?.data?.data)) setDeptSummary(deptRes.value.data.data);
        if (ticketRes.status === 'fulfilled' && ticketRes.value?.data?.data) setTicketSummary(ticketRes.value.data.data);
        if (availRes.status === 'fulfilled' && availRes.value?.data?.data != null) setAssetAvailable(availRes.value.data.data);
        if (assignedRes.status === 'fulfilled' && assignedRes.value?.data?.data != null) setAssetAssigned(assignedRes.value.data.data);
        if (notifRes.status === 'fulfilled' && notifRes.value?.data?.data) setNotificationSummary(notifRes.value.data.data);
      } catch (e) {
        setError('Some dashboard data could not be loaded. Backend services may still be starting up.');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const ticketChartData = Object.entries(ticketSummary).map(([k, v]) => ({ name: k, value: v }));
  const openTickets = (ticketSummary.OPEN || 0) + (ticketSummary.ASSIGNED || 0) + (ticketSummary.IN_PROGRESS || 0);

  return (
    <Box>
      <PageHeader
        title={`Welcome back, ${user?.firstName || 'there'}`}
        subtitle="Here's what's happening across the platform today."
      />

      {error && <Alert severity="warning" sx={{ mb: 3 }}>{error}</Alert>}

      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          {loading ? <Skeleton variant="rounded" height={90} /> :
            <StatCard icon={PeopleAltRoundedIcon} label="Active employees" value={activeEmployees} />}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {loading ? <Skeleton variant="rounded" height={90} /> :
            <StatCard icon={ConfirmationNumberRoundedIcon} label="Open tickets" value={openTickets} tone="warning.main" />}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {loading ? <Skeleton variant="rounded" height={90} /> :
            <StatCard icon={LaptopMacRoundedIcon} label="Assets assigned" value={assetAssigned} tone="success.main" />}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {loading ? <Skeleton variant="rounded" height={90} /> :
            <StatCard icon={NotificationsRoundedIcon} label="Notifications sent" value={notificationSummary.SENT || 0} tone="info.main" />}
        </Grid>
      </Grid>

      <Grid container spacing={2.5}>
        <Grid item xs={12} md={7}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>Employees by department</Typography>
              {loading ? <Skeleton variant="rounded" height={280} /> : (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={deptSummary}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.15} />
                    <XAxis dataKey="department" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="employeeCount" radius={[6, 6, 0, 0]}>
                      {deptSummary.map((entry, index) => (
                        <Cell key={entry.department} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={5}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>Ticket status breakdown</Typography>
              {loading ? <Skeleton variant="rounded" height={280} /> : (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie data={ticketChartData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                      {ticketChartData.map((entry, index) => (
                        <Cell key={entry.name} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
