import { Chip } from '@mui/material';

const COLOR_MAP = {
  ACTIVE: 'success', APPROVED: 'success', SENT: 'success', PAID: 'success',
  AVAILABLE: 'success', RESOLVED: 'success', CLOSED: 'default', GENERATED: 'info',
  PENDING: 'warning', RETRYING: 'warning', ASSIGNED: 'info', IN_PROGRESS: 'info',
  OPEN: 'info', ON_LEAVE: 'warning', UNDER_REPAIR: 'warning', REOPENED: 'warning',
  REJECTED: 'error', FAILED: 'error', CANCELLED: 'default', SUSPENDED: 'error',
  TERMINATED: 'default', RETIRED: 'default'
};

export default function StatusChip({ status, size = 'small' }) {
  if (!status) return null;
  const color = COLOR_MAP[status] || 'default';
  return <Chip label={status.replace(/_/g, ' ')} color={color} size={size} variant="outlined" />;
}
