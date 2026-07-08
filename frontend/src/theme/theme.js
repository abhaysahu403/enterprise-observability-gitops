import { createTheme } from '@mui/material/styles';

// Design language: "control room". This is an operations platform for
// monitoring a whole enterprise, so the palette leans on the visual
// vocabulary of a NOC/observability dashboard rather than generic SaaS
// blue: deep graphite surfaces, a signal-cyan primary (status-light cyan,
// not Material default blue), amber for warnings, and a monospace face
// for anything numeric (ids, codes, currency, timestamps) to reinforce
// that this is a data-dense instrument panel, not a marketing site.

const fontDisplay = "'Manrope', 'Segoe UI', sans-serif";
const fontMono = "'IBM Plex Mono', 'Roboto Mono', monospace";

const palette = {
  graphite: '#12161C',
  graphiteRaised: '#1A2027',
  paper: '#F5F7F8',
  paperRaised: '#FFFFFF',
  cyan: '#1FB6C9',
  cyanDark: '#0E8C9C',
  amber: '#F2A93B',
  coral: '#E4572E',
  mint: '#2FB673',
  slateText: '#5B6470'
};

export function buildTheme(mode) {
  const isDark = mode === 'dark';

  return createTheme({
    palette: {
      mode,
      primary: { main: palette.cyan, dark: palette.cyanDark, contrastText: '#04181A' },
      secondary: { main: palette.amber, contrastText: '#241300' },
      error: { main: palette.coral },
      warning: { main: palette.amber },
      success: { main: palette.mint },
      background: {
        default: isDark ? palette.graphite : palette.paper,
        paper: isDark ? palette.graphiteRaised : palette.paperRaised
      },
      text: {
        primary: isDark ? '#E7ECEF' : '#1A2027',
        secondary: isDark ? '#9AA6B2' : palette.slateText
      },
      divider: isDark ? 'rgba(231,236,239,0.09)' : 'rgba(26,32,39,0.09)'
    },
    shape: { borderRadius: 10 },
    typography: {
      fontFamily: fontDisplay,
      h1: { fontFamily: fontDisplay, fontWeight: 800 },
      h2: { fontFamily: fontDisplay, fontWeight: 800 },
      h3: { fontFamily: fontDisplay, fontWeight: 700 },
      h4: { fontFamily: fontDisplay, fontWeight: 700 },
      h5: { fontFamily: fontDisplay, fontWeight: 700 },
      h6: { fontFamily: fontDisplay, fontWeight: 700 },
      button: { fontWeight: 700, textTransform: 'none', letterSpacing: 0.2 },
      overline: { fontFamily: fontMono, letterSpacing: 1.2 }
    },
    custom: { fontMono, palette },
    components: {
      MuiPaper: {
        styleOverrides: {
          root: { backgroundImage: 'none' }
        }
      },
      MuiButton: {
        styleOverrides: {
          root: { borderRadius: 8, paddingInline: 16 }
        }
      },
      MuiChip: {
        styleOverrides: {
          root: { fontFamily: fontMono, fontWeight: 500, fontSize: '0.72rem' }
        }
      },
      MuiCard: {
        styleOverrides: {
          root: {
            border: isDark ? '1px solid rgba(231,236,239,0.08)' : '1px solid rgba(26,32,39,0.08)'
          }
        }
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundColor: isDark ? palette.graphite : palette.paperRaised,
            borderRight: isDark ? '1px solid rgba(231,236,239,0.08)' : '1px solid rgba(26,32,39,0.08)'
          }
        }
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: isDark ? palette.graphiteRaised : palette.paperRaised,
            color: isDark ? '#E7ECEF' : '#1A2027',
            boxShadow: 'none',
            borderBottom: isDark ? '1px solid rgba(231,236,239,0.08)' : '1px solid rgba(26,32,39,0.08)'
          }
        }
      }
    }
  });
}
