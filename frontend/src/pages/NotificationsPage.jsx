import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, TextField, MenuItem, Stack, Table, TableHead, TableRow, TableCell,
  TableBody, Card, IconButton, Tooltip, Grid, CardContent, Typography
} from '@mui/material';
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { NotificationAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';

export default function NotificationsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState([]);
  const [summary, setSummary] = useState({});
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [historyRes, summaryRes] = await Promise.all([
        NotificationAPI.history({ status: statusFilter || undefined, size: 20, sort: 'createdAt,desc' }),
        NotificationAPI.statusSummary()
      ]);
      if (historyRes?.data?.data?.content && Array.isArray(historyRes.data.data.content)) {
        setRows(historyRes.data.data.content);
      } else {
        setRows([]);
      }
      if (summaryRes?.data?.data) {
        setSummary(summaryRes.data.data);
      } else {
        setSummary({});
      }
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
      setRows([]);
      setSummary({});
    } finally {
      setLoading(false);
    }
  }, [statusFilter, enqueueSnackbar]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleRetry = async (id) => {
    try {
      await NotificationAPI.retry(id);
      enqueueSnackbar('Retry attempted', { variant: 'success' });
      fetchData();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  return (
    <Box>
      <PageHeader title="Notifications" subtitle="Delivery history across email, Teams, and SMS channels." />

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {Object.entries(summary).map(([status, count]) => (
          <Grid item xs={6} sm={3} key={status}>
            <Card>
              <CardContent>
                <Typography variant="caption" color="text.secondary">{status}</Typography>
                <Typography variant="h5" fontWeight={800} sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{count}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} size="small" sx={{ minWidth: 180 }}>
          <MenuItem value="">All statuses</MenuItem>
          {['PENDING', 'SENT', 'RETRYING', 'FAILED'].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
      </Stack>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Recipient</TableCell>
              <TableCell>Channel</TableCell>
              <TableCell>Subject / Message</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Retries</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((n) => (
              <TableRow key={n.id} hover>
                <TableCell>{n.recipientName}</TableCell>
                <TableCell>{n.channel}</TableCell>
                <TableCell sx={{ maxWidth: 280 }}>{n.subject || n.message}</TableCell>
                <TableCell><StatusChip status={n.status} /></TableCell>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{n.retryCount}/{n.maxRetries}</TableCell>
                <TableCell align="right">
                  {(n.status === 'FAILED' || n.status === 'RETRYING') && n.retryCount < n.maxRetries && (
                    <Tooltip title="Retry"><IconButton size="small" onClick={() => handleRetry(n.id)}><ReplayRoundedIcon fontSize="small" /></IconButton></Tooltip>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
    </Box>
  );
}
