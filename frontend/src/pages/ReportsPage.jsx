import { useEffect, useState } from 'react';
import { Box, Grid, Card, CardContent, Typography } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import PageHeader from '../components/PageHeader';
import { EmployeeAPI, TicketAPI, AssetAPI, NotificationAPI } from '../api/endpoints';

const COLORS = ['#1FB6C9', '#F2A93B', '#2FB673', '#E4572E', '#8B7FD6', '#5B6470'];

function ReportCard({ title, data }) {
  return (
    <Grid item xs={12} md={6}>
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>{title}</Typography>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={data} dataKey="value" nameKey="name" outerRadius={90} label>
                {data.map((entry, i) => <Cell key={entry.name} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </Grid>
  );
}

export default function ReportsPage() {
  const [deptData, setDeptData] = useState([]);
  const [ticketData, setTicketData] = useState([]);
  const [notificationData, setNotificationData] = useState([]);
  const [assetData, setAssetData] = useState([]);

  useEffect(() => {
    (async () => {
      try {
        const deptRes = await EmployeeAPI.departmentSummary();
        setDeptData(deptRes.data.data.map((d) => ({ name: d.department, value: d.employeeCount })));
      } catch { /* service may be unavailable */ }

      try {
        const ticketRes = await TicketAPI.statusSummary();
        setTicketData(Object.entries(ticketRes.data.data).map(([k, v]) => ({ name: k, value: v })));
      } catch { /* service may be unavailable */ }

      try {
        const notifRes = await NotificationAPI.statusSummary();
        setNotificationData(Object.entries(notifRes.data.data).map(([k, v]) => ({ name: k, value: v })));
      } catch { /* service may be unavailable */ }

      try {
        const [avail, assigned] = await Promise.all([
          AssetAPI.countByStatus('AVAILABLE'),
          AssetAPI.countByStatus('ASSIGNED')
        ]);
        setAssetData([
          { name: 'AVAILABLE', value: avail.data.data },
          { name: 'ASSIGNED', value: assigned.data.data }
        ]);
      } catch { /* service may be unavailable */ }
    })();
  }, []);

  return (
    <Box>
      <PageHeader title="Reports" subtitle="Cross-service analytics for headcount, tickets, assets, and notifications." />
      <Grid container spacing={2.5}>
        <ReportCard title="Employees by department" data={deptData} />
        <ReportCard title="Tickets by status" data={ticketData} />
        <ReportCard title="Assets by status" data={assetData} />
        <ReportCard title="Notifications by delivery status" data={notificationData} />
      </Grid>
    </Box>
  );
}
