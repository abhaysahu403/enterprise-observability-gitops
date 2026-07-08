import { useEffect, useState, useCallback } from 'react';
import {
  Box, Button, TextField, MenuItem, Stack, Dialog, DialogTitle, DialogContent, DialogActions,
  Grid, IconButton, Tooltip, Chip
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { EmployeeAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

const DEPARTMENTS = ['HR', 'FINANCE', 'ENGINEERING', 'OPERATIONS', 'SALES', 'SUPPORT'];
const STATUSES = ['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'];
const GRADES = ['L1', 'L2', 'L3', 'L4', 'L5', 'L6', 'M1', 'M2', 'M3'];

const emptyForm = {
  firstName: '', lastName: '', email: '', phone: '', address: '',
  department: 'ENGINEERING', designation: '', managerId: '', joiningDate: '',
  status: 'ACTIVE', salaryGrade: 'L1'
};

export default function EmployeesPage() {
  const { hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const canEdit = hasRole('ADMIN', 'HR');

  const [rows, setRows] = useState([]);
  const [rowCount, setRowCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [department, setDepartment] = useState('');
  const [status, setStatus] = useState('');
  const [paginationModel, setPaginationModel] = useState({ page: 0, pageSize: 10 });

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  const fetchEmployees = useCallback(async () => {
    setLoading(true);
    try {
      const res = await EmployeeAPI.list({
        search: search || undefined,
        department: department || undefined,
        status: status || undefined,
        page: paginationModel.page,
        size: paginationModel.pageSize,
        sort: 'id,desc'
      });
      if (res?.data?.data?.content && Array.isArray(res.data.data.content)) {
        setRows(res.data.data.content);
        setRowCount(res.data.data.totalElements || 0);
      } else {
        setRows([]);
        setRowCount(0);
      }
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
      setRows([]);
      setRowCount(0);
    } finally {
      setLoading(false);
    }
  }, [search, department, status, paginationModel, enqueueSnackbar]);

  useEffect(() => { fetchEmployees(); }, [fetchEmployees]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEdit = (row) => {
    setEditingId(row.id);
    setForm({
      firstName: row.firstName, lastName: row.lastName, email: row.email,
      phone: row.phone || '', address: row.address || '', department: row.department,
      designation: row.designation, managerId: row.managerId || '', joiningDate: row.joiningDate,
      status: row.status, salaryGrade: row.salaryGrade
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const payload = { ...form, managerId: form.managerId ? Number(form.managerId) : null };
      if (editingId) {
        await EmployeeAPI.update(editingId, payload);
        enqueueSnackbar('Employee updated', { variant: 'success' });
      } else {
        await EmployeeAPI.create(payload);
        enqueueSnackbar('Employee created', { variant: 'success' });
      }
      setDialogOpen(false);
      fetchEmployees();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this employee record? This cannot be undone.')) return;
    try {
      await EmployeeAPI.remove(id);
      enqueueSnackbar('Employee deleted', { variant: 'success' });
      fetchEmployees();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  const columns = [
    { field: 'employeeCode', headerName: 'Code', width: 110 },
    { field: 'firstName', headerName: 'First Name', flex: 1, minWidth: 140 },
    { field: 'lastName', headerName: 'Last Name', flex: 1, minWidth: 140 },
    { field: 'email', headerName: 'Email', flex: 1, minWidth: 180 },
    { field: 'department', headerName: 'Department', width: 130 },
    { field: 'designation', headerName: 'Designation', width: 180 },
    { field: 'managerName', headerName: 'Manager', width: 150 },
    {
      field: 'status', headerName: 'Status', width: 130,
      renderCell: (params) => <StatusChip status={params.value} />
    },
    { field: 'salaryGrade', headerName: 'Grade', width: 90 },
    ...(canEdit ? [{
      field: 'actions', headerName: '', width: 100, sortable: false, filterable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Edit"><IconButton size="small" onClick={() => openEdit(params.row)}><EditRoundedIcon fontSize="small" /></IconButton></Tooltip>
          {hasRole('ADMIN') &&
            <Tooltip title="Delete"><IconButton size="small" onClick={() => handleDelete(params.row.id)}><DeleteRoundedIcon fontSize="small" /></IconButton></Tooltip>}
        </Stack>
      )
    }] : [])
  ];

  return (
    <Box>
      <PageHeader
        title="Employees"
        subtitle="Search, filter, and manage employee records across the organization."
        action={canEdit && (
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={openCreate}>
            Add employee
          </Button>
        )}
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <TextField
          placeholder="Search name, email, code…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size="small"
          sx={{ minWidth: 260 }}
        />
        <TextField select label="Department" value={department} onChange={(e) => setDepartment(e.target.value)} size="small" sx={{ minWidth: 160 }}>
          <MenuItem value="">All departments</MenuItem>
          {DEPARTMENTS.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
        </TextField>
        <TextField select label="Status" value={status} onChange={(e) => setStatus(e.target.value)} size="small" sx={{ minWidth: 160 }}>
          <MenuItem value="">All statuses</MenuItem>
          {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
        </TextField>
      </Stack>

      <Box sx={{ height: 560, bgcolor: 'background.paper', borderRadius: 2 }}>
        <DataGrid
          rows={rows}
          columns={columns}
          rowCount={rowCount}
          loading={loading}
          paginationMode="server"
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          pageSizeOptions={[10, 20, 50]}
          disableRowSelectionOnClick
          sx={{ border: 'none' }}
        />
      </Box>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit employee' : 'Add employee'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0.5 }}>
            <Grid item xs={6}>
              <TextField label="First name" fullWidth value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Last name" fullWidth value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Email" fullWidth value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Phone" fullWidth value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Joining date" type="date" fullWidth InputLabelProps={{ shrink: true }} value={form.joiningDate} onChange={(e) => setForm({ ...form, joiningDate: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Address" fullWidth value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField select label="Department" fullWidth value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}>
                {DEPARTMENTS.map((d) => <MenuItem key={d} value={d}>{d}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField label="Designation" fullWidth value={form.designation} onChange={(e) => setForm({ ...form, designation: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField label="Manager id (optional)" fullWidth value={form.managerId} onChange={(e) => setForm({ ...form, managerId: e.target.value })} />
            </Grid>
            <Grid item xs={6}>
              <TextField select label="Salary grade" fullWidth value={form.salaryGrade} onChange={(e) => setForm({ ...form, salaryGrade: e.target.value })}>
                {GRADES.map((g) => <MenuItem key={g} value={g}>{g}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={6}>
              <TextField select label="Status" fullWidth value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
              </TextField>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
