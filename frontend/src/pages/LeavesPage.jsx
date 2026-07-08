import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, TextField, MenuItem, Stack, Dialog, DialogTitle, DialogContent, DialogActions,
  Grid, Card, CardContent, Typography, Table, TableHead, TableRow, TableCell, TableBody, IconButton, Tooltip
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { LeaveAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

const LEAVE_TYPES = ['ANNUAL', 'CASUAL', 'MEDICAL', 'WORK_FROM_HOME'];

export default function LeavesPage() {
  const { user, hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const canApprove = hasRole('ADMIN', 'MANAGER', 'HR');

  const [rows, setRows] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ leaveType: 'ANNUAL', startDate: '', endDate: '', reason: '' });

  // In this demo, employee id 7 (Myra Chopra) is used as the "acting employee"
  // context for applying/cancelling leave, since auth users and employee
  // records are managed as separate microservices.
  const actingEmployeeId = 7;
  const actingEmployeeName = 'Myra Chopra';

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [historyRes, balanceRes] = await Promise.all([
        LeaveAPI.history({ status: statusFilter || undefined, size: 20, sort: 'createdAt,desc' }),
        LeaveAPI.balance(actingEmployeeId)
      ]);
      if (historyRes?.data?.data?.content && Array.isArray(historyRes.data.data.content)) {
        setRows(historyRes.data.data.content);
      } else {
        setRows([]);
      }
      if (balanceRes?.data?.data) {
        setBalances(balanceRes.data.data);
      } else {
        setBalances({});
      }
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
      setRows([]);
      setBalances({});
    } finally {
      setLoading(false);
    }
  }, [statusFilter, enqueueSnackbar]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleApply = async () => {
    setSaving(true);
    try {
      await LeaveAPI.apply({
        employeeId: actingEmployeeId,
        employeeName: actingEmployeeName,
        ...form
      });
      enqueueSnackbar('Leave request submitted', { variant: 'success' });
      setDialogOpen(false);
      setForm({ leaveType: 'ANNUAL', startDate: '', endDate: '', reason: '' });
      fetchData();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDecision = async (id, decision) => {
    try {
      const payload = { approverId: user?.id || 1, approverName: `${user?.firstName} ${user?.lastName}`, comment: '' };
      if (decision === 'approve') await LeaveAPI.approve(id, payload);
      else await LeaveAPI.reject(id, payload);
      enqueueSnackbar(`Leave ${decision === 'approve' ? 'approved' : 'rejected'}`, { variant: 'success' });
      fetchData();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  return (
    <Box>
      <PageHeader
        title="Leave Management"
        subtitle="Apply for leave, track balances, and review requests."
        action={
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setDialogOpen(true)}>
            Apply for leave
          </Button>
        }
      />

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {balances.map((b) => (
          <Grid item xs={6} sm={3} key={b.leaveType}>
            <Card>
              <CardContent>
                <Typography variant="caption" color="text.secondary">{b.leaveType.replace(/_/g, ' ')}</Typography>
                <Typography variant="h5" fontWeight={800} sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>
                  {b.available}<Typography component="span" variant="body2" color="text.secondary"> / {b.totalAllocated}</Typography>
                </Typography>
                <Typography variant="caption" color="text.secondary">days available</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} size="small" sx={{ minWidth: 180 }}>
          <MenuItem value="">All statuses</MenuItem>
          {['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
      </Stack>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Dates</TableCell>
              <TableCell>Days</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Reason</TableCell>
              {canApprove && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell>{r.employeeName}</TableCell>
                <TableCell>{r.leaveType.replace(/_/g, ' ')}</TableCell>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace', fontSize: '0.8rem' }}>{r.startDate} → {r.endDate}</TableCell>
                <TableCell>{r.totalDays}</TableCell>
                <TableCell><StatusChip status={r.status} /></TableCell>
                <TableCell sx={{ maxWidth: 220 }}>{r.reason}</TableCell>
                {canApprove && (
                  <TableCell align="right">
                    {r.status === 'PENDING' && (
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                        <Tooltip title="Approve"><IconButton size="small" color="success" onClick={() => handleDecision(r.id, 'approve')}><CheckRoundedIcon fontSize="small" /></IconButton></Tooltip>
                        <Tooltip title="Reject"><IconButton size="small" color="error" onClick={() => handleDecision(r.id, 'reject')}><CloseRoundedIcon fontSize="small" /></IconButton></Tooltip>
                      </Stack>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Apply for leave</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 0.5 }}>
            <TextField select label="Leave type" value={form.leaveType} onChange={(e) => setForm({ ...form, leaveType: e.target.value })}>
              {LEAVE_TYPES.map((t) => <MenuItem key={t} value={t}>{t.replace(/_/g, ' ')}</MenuItem>)}
            </TextField>
            <TextField label="Start date" type="date" InputLabelProps={{ shrink: true }} value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            <TextField label="End date" type="date" InputLabelProps={{ shrink: true }} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
            <TextField label="Reason" multiline rows={3} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleApply} disabled={saving}>
            {saving ? 'Submitting…' : 'Submit'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
