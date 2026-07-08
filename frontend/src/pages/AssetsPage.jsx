import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, TextField, MenuItem, Stack, Table, TableHead, TableRow, TableCell,
  TableBody, Card, Dialog, DialogTitle, DialogContent, DialogActions, Grid, IconButton, Tooltip
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import AssignmentIndRoundedIcon from '@mui/icons-material/AssignmentIndRounded';
import AssignmentReturnRoundedIcon from '@mui/icons-material/AssignmentReturnRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { AssetAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

const TYPES = ['LAPTOP', 'DESKTOP', 'MONITOR', 'KEYBOARD', 'MOUSE', 'PHONE'];
const STATUSES = ['AVAILABLE', 'ASSIGNED', 'UNDER_REPAIR', 'RETIRED'];

export default function AssetsPage() {
  const { hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const canEdit = hasRole('ADMIN', 'HR');

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({ type: 'LAPTOP', model: '', vendor: '', purchaseDate: '', warrantyExpiry: '' });

  const [assignOpen, setAssignOpen] = useState(false);
  const [assignTarget, setAssignTarget] = useState(null);
  const [assignForm, setAssignForm] = useState({ employeeId: '', employeeName: '' });

  const [saving, setSaving] = useState(false);

  const fetchAssets = useCallback(async () => {
    setLoading(true);
    try {
      const res = await AssetAPI.list({ status: statusFilter || undefined, type: typeFilter || undefined, size: 20, sort: 'id,desc' });
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
  }, [statusFilter, typeFilter, enqueueSnackbar]);

  useEffect(() => { fetchAssets(); }, [fetchAssets]);

  const handleCreate = async () => {
    setSaving(true);
    try {
      await AssetAPI.create(createForm);
      enqueueSnackbar('Asset registered', { variant: 'success' });
      setCreateOpen(false);
      setCreateForm({ type: 'LAPTOP', model: '', vendor: '', purchaseDate: '', warrantyExpiry: '' });
      fetchAssets();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const openAssign = (row) => {
    setAssignTarget(row);
    setAssignForm({ employeeId: '', employeeName: '' });
    setAssignOpen(true);
  };

  const handleAssign = async () => {
    setSaving(true);
    try {
      await AssetAPI.assign(assignTarget.id, { employeeId: Number(assignForm.employeeId), employeeName: assignForm.employeeName });
      enqueueSnackbar('Asset assigned', { variant: 'success' });
      setAssignOpen(false);
      fetchAssets();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleReturn = async (id) => {
    try {
      await AssetAPI.returnAsset(id);
      enqueueSnackbar('Asset returned', { variant: 'success' });
      fetchAssets();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  return (
    <Box>
      <PageHeader
        title="Asset Management"
        subtitle="Track laptops, monitors, and other company equipment."
        action={canEdit && (
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setCreateOpen(true)}>
            Register asset
          </Button>
        )}
      />

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select label="Type" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} size="small" sx={{ minWidth: 160 }}>
          <MenuItem value="">All types</MenuItem>
          {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </TextField>
        <TextField select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} size="small" sx={{ minWidth: 160 }}>
          <MenuItem value="">All statuses</MenuItem>
          {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
      </Stack>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Tag</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Model</TableCell>
              <TableCell>Vendor</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Assigned to</TableCell>
              {canEdit && <TableCell align="right">Actions</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((a) => (
              <TableRow key={a.id} hover>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{a.assetTag}</TableCell>
                <TableCell>{a.type}</TableCell>
                <TableCell>{a.model}</TableCell>
                <TableCell>{a.vendor}</TableCell>
                <TableCell><StatusChip status={a.status} /></TableCell>
                <TableCell>{a.assignedToName || '—'}</TableCell>
                {canEdit && (
                  <TableCell align="right">
                    {a.status === 'AVAILABLE' && (
                      <Tooltip title="Assign"><IconButton size="small" onClick={() => openAssign(a)}><AssignmentIndRoundedIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                    {a.status === 'ASSIGNED' && (
                      <Tooltip title="Return"><IconButton size="small" onClick={() => handleReturn(a.id)}><AssignmentReturnRoundedIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Register asset</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={12}>
              <TextField select label="Type" fullWidth value={createForm.type} onChange={(e) => setCreateForm({ ...createForm, type: e.target.value })}>
                {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12}><TextField label="Model" fullWidth value={createForm.model} onChange={(e) => setCreateForm({ ...createForm, model: e.target.value })} /></Grid>
            <Grid item xs={12}><TextField label="Vendor" fullWidth value={createForm.vendor} onChange={(e) => setCreateForm({ ...createForm, vendor: e.target.value })} /></Grid>
            <Grid item xs={6}><TextField label="Purchase date" type="date" InputLabelProps={{ shrink: true }} fullWidth value={createForm.purchaseDate} onChange={(e) => setCreateForm({ ...createForm, purchaseDate: e.target.value })} /></Grid>
            <Grid item xs={6}><TextField label="Warranty expiry" type="date" InputLabelProps={{ shrink: true }} fullWidth value={createForm.warrantyExpiry} onChange={(e) => setCreateForm({ ...createForm, warrantyExpiry: e.target.value })} /></Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={saving}>{saving ? 'Saving…' : 'Register'}</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={assignOpen} onClose={() => setAssignOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Assign {assignTarget?.assetTag}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 0.5 }}>
            <TextField label="Employee ID" value={assignForm.employeeId} onChange={(e) => setAssignForm({ ...assignForm, employeeId: e.target.value })} />
            <TextField label="Employee name" value={assignForm.employeeName} onChange={(e) => setAssignForm({ ...assignForm, employeeName: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAssignOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAssign} disabled={saving}>{saving ? 'Assigning…' : 'Assign'}</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
