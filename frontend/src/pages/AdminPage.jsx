import { useEffect, useState, useCallback } from 'react';
import {
  Box, Table, TableHead, TableRow, TableCell, TableBody, Card, Chip, Switch,
  TextField, Stack
} from '@mui/material';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import { AuthAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';

export default function AdminPage() {
  const { enqueueSnackbar } = useSnackbar();
  const [rows, setRows] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await AuthAPI.listUsers({ search: search || undefined, size: 50 });
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
  }, [search, enqueueSnackbar]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const toggleEnabled = async (row) => {
    try {
      await AuthAPI.updateUser(row.id, { enabled: !row.enabled });
      enqueueSnackbar(`${row.username} ${!row.enabled ? 'enabled' : 'disabled'}`, { variant: 'success' });
      fetchUsers();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    }
  };

  return (
    <Box>
      <PageHeader title="Admin · User Management" subtitle="Manage platform accounts, roles, and access." />

      <Stack sx={{ mb: 2 }}>
        <TextField
          placeholder="Search username or email…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size="small"
          sx={{ maxWidth: 320 }}
        />
      </Stack>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Roles</TableCell>
              <TableCell>Enabled</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((u) => (
              <TableRow key={u.id} hover>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{u.username}</TableCell>
                <TableCell>{u.firstName} {u.lastName}</TableCell>
                <TableCell>{u.email}</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.5}>
                    {u.roles?.map((r) => <Chip key={r} label={r} size="small" />)}
                  </Stack>
                </TableCell>
                <TableCell>
                  <Switch checked={u.enabled} onChange={() => toggleEnabled(u)} size="small" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
    </Box>
  );
}
