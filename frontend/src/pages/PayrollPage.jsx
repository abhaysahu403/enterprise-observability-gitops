import { useEffect, useState } from 'react';
import {
  Box, Button, TextField, Grid, Card, CardContent, Typography, Table, TableHead,
  TableRow, TableCell, TableBody, Stack, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded';
import { useSnackbar } from 'notistack';
import PageHeader from '../components/PageHeader';
import StatusChip from '../components/StatusChip';
import { PayrollAPI } from '../api/endpoints';
import { extractErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

const currency = (v) => `₹${Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

export default function PayrollPage() {
  const { hasRole } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const canGenerate = hasRole('ADMIN', 'HR');

  const [employeeId, setEmployeeId] = useState('1');
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [genDialogOpen, setGenDialogOpen] = useState(false);
  const [genMonth, setGenMonth] = useState(new Date().getMonth() + 1);
  const [genYear, setGenYear] = useState(new Date().getFullYear());
  const [generating, setGenerating] = useState(false);

  const fetchHistory = async () => {
    if (!employeeId) return;
    setLoading(true);
    try {
      const res = await PayrollAPI.history({ employeeId, size: 12, sort: 'payYear,desc' });
      if (res?.data?.data?.content && Array.isArray(res.data.data.content)) {
        setHistory(res.data.data.content);
      } else {
        setHistory([]);
      }
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
      setHistory([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchHistory(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGenerate = async () => {
    setGenerating(true);
    try {
      const res = await PayrollAPI.generate({ month: Number(genMonth), year: Number(genYear) });
      const summary = res.data.data;
      enqueueSnackbar(`Payroll generated: ${summary.processedCount} processed, ${summary.failedCount} failed`, { variant: 'success' });
      setGenDialogOpen(false);
      fetchHistory();
    } catch (e) {
      enqueueSnackbar(extractErrorMessage(e), { variant: 'error' });
    } finally {
      setGenerating(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Payroll"
        subtitle="Salary history, payslips, and monthly payroll generation."
        action={canGenerate && (
          <Button variant="contained" startIcon={<PlayArrowRoundedIcon />} onClick={() => setGenDialogOpen(true)}>
            Run payroll
          </Button>
        )}
      />

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField label="Employee ID" size="small" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} sx={{ width: 200 }} />
            <Button variant="outlined" onClick={fetchHistory} disabled={loading}>
              {loading ? 'Loading…' : 'Load salary history'}
            </Button>
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {history.slice(0, 3).map((h) => (
          <Grid item xs={12} sm={4} key={h.id}>
            <Card>
              <CardContent>
                <Typography variant="caption" color="text.secondary">{h.payMonth}/{h.payYear}</Typography>
                <Typography variant="h6" fontWeight={800} sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{currency(h.netSalary)}</Typography>
                <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
                  <Typography variant="caption" color="text.secondary">Net pay</Typography>
                  <StatusChip status={h.status} />
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Period</TableCell>
              <TableCell>Basic</TableCell>
              <TableCell>Bonus</TableCell>
              <TableCell>Gross</TableCell>
              <TableCell>Tax</TableCell>
              <TableCell>Net</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {history.map((h) => (
              <TableRow key={h.id} hover>
                <TableCell sx={{ fontFamily: 'IBM Plex Mono, monospace' }}>{h.payMonth}/{h.payYear}</TableCell>
                <TableCell>{currency(h.basicSalary)}</TableCell>
                <TableCell>{currency(h.bonus)}</TableCell>
                <TableCell>{currency(h.grossSalary)}</TableCell>
                <TableCell>{currency(h.taxDeducted)}</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>{currency(h.netSalary)}</TableCell>
                <TableCell><StatusChip status={h.status} /></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={genDialogOpen} onClose={() => setGenDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Run monthly payroll</DialogTitle>
        <DialogContent>
          <Stack direction="row" spacing={2} sx={{ mt: 0.5 }}>
            <TextField label="Month" type="number" value={genMonth} onChange={(e) => setGenMonth(e.target.value)} inputProps={{ min: 1, max: 12 }} />
            <TextField label="Year" type="number" value={genYear} onChange={(e) => setGenYear(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setGenDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleGenerate} disabled={generating}>
            {generating ? 'Running…' : 'Run'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
