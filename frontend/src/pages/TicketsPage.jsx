import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, TextField, MenuItem, Stack, Table, TableHead, TableRow, TableCell,
  TableBody, Card, Dialog, DialogTitle, DialogContent, DialogActions, Grid, Typography, Divider
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { TicketAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';

const CATEGORIES = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'ACCESS', 'HR_QUERY', 'PAYROLL_QUERY', 'OTHER'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const emptyForm = { title: '', description: '', category: 'SOFTWARE', priority: 'MEDIUM', raisedByEmployeeId: '', raisedByName: '' };

export default function TicketsPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState(null);
  const [resolution, setResolution] = useState('');

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const res = await TicketAPI.list({ status: statusFilter || undefined, size: 20, sort: 'createdAt,desc' });
      if (res?.data?.data?.content && Array.isArray(res.data.data.content)) {
        setRows(res.data.data.content);
      } else {
        setRows([]);
      }
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, enqueueSnackbar]);

  useEffect(() => { fetchTickets(); }, [fetchTickets]);

  const handleCreate = async () => {
    setSaving(true);
    try {
      await TicketAPI.create({ ...form, raisedByEmployeeId: Number(form.raisedByEmployeeId) });
      enqueueSnackbar('Ticket raised', { variant: 'success' });
      setCreateOpen(false);
      setForm(emptyForm);
      fetchTickets();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const openDetail = async (row) => {
    setDetailOpen(true);
    setResolution('');
    try {
      const res = await TicketAPI.get(row.id);
      setDetail(res.data.data);
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  const handleResolve = async () => {
    try {
      await TicketAPI.resolve(detail.id, { resolution });
      enqueueSnackbar('Ticket resolved', { variant: 'success' });
      setDetailOpen(false);
      fetchTickets();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  return (
    <Box>
      <PageHeader
        title="Help Desk"
        subtitle="Raise and track support tickets with SLA monitoring."
        action={
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setCreateOpen(true)}>
            Raise ticket
          </Button>
        }
      />

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} size="small" sx={{ minWidth: 180 }}>
          <MenuItem value="">All statuses</MenuItem>
          {['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
      </Stack>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Ticket #</TableCell>
              <TableCell>Title</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>SLA</TableCell>
              <TableCell>Raised by</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((t) => (
              <TableRow key={t.id} hover onClick={() => openDetail(t)} sx={{ cursor: 'pointer' }}>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{t.ticketNumber}</TableCell>
                <TableCell>{t.title}</TableCell>
                <TableCell>{t.category}</TableCell>
                <TableCell><StatusChip status={t.priority} /></TableCell>
                <TableCell><StatusChip status={t.status} /></TableCell>
                <TableCell>{t.slaBreached ? <StatusChip status="FAILED" /> : <StatusChip status="ACTIVE" />}</TableCell>
                <TableCell>{t.raisedByName}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Raise a ticket</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}><TextField label="Title" fullWidth value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Grid>
            <Grid item xs={12}><TextField label="Description" multiline rows={3} fullWidth value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Grid>
            <Grid item xs={6}>
              <TextField select label="Category" fullWidth value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {CATEGORIES.map((c) => <MenuItem key={c} value={c}>{c.replace(/_/g, ' ')}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField select label="Priority" fullWidth value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                {PRIORITIES.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}><TextField label="Your employee ID" fullWidth value={form.raisedByEmployeeId} onChange={(e) => setForm({ ...form, raisedByEmployeeId: e.target.value })} /></Grid>
            <Grid item xs={6}><TextField label="Your name" fullWidth value={form.raisedByName} onChange={(e) => setForm({ ...form, raisedByName: e.target.value })} /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={saving}>{saving ? 'Submitting…' : 'Submit'}</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="sm" fullWidth>
        {detail && (
          <>
            <DialogTitle>{detail.ticketNumber} · {detail.title}</DialogTitle>
            <DialogContent>
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <StatusChip status={detail.status} />
                <StatusChip status={detail.priority} />
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{detail.description}</Typography>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1 }}>Comments</Typography>
              <Stack spacing={1} sx={{ maxHeight: 160, overflowY: 'auto', mb: 2 }}>
                {(detail.comments || []).map((c) => (
                  <Box key={c.id} sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1.5 }}>
                    <Typography variant="caption" fontWeight={700}>{c.authorName}</Typography>
                    <Typography variant="body2">{c.comment}</Typography>
                  </Box>
                ))}
                {(!detail.comments || detail.comments.length === 0) && (
                  <Typography variant="caption" color="text.secondary">No comments yet.</Typography>
                )}
              </Stack>
              {detail.status !== 'RESOLVED' && detail.status !== 'CLOSED' && (
                <TextField
                  label="Resolution notes"
                  fullWidth multiline rows={2}
                  value={resolution}
                  onChange={(e) => setResolution(e.target.value)}
                />
              )}
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
              <Button onClick={() => setDetailOpen(false)}>Close</Button>
              {detail.status !== 'RESOLVED' && detail.status !== 'CLOSED' && (
                <Button variant="contained" onClick={handleResolve} disabled={!resolution}>Resolve</Button>
              )}
            </DialogActions>
          </>
        )}
      </Dialog>
    </Box>
  );
}
