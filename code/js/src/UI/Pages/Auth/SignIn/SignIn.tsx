import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import FormLabel from '@mui/material/FormLabel';
import FormControl from '@mui/material/FormControl';
import Link from '@mui/material/Link';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import MuiCard from '@mui/material/Card';
import { styled } from '@mui/material/styles';
import { useForm } from '../../../Components/State/useForm';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert } from '@mui/material';
import { AuthService } from '../../../../Services/auth/AuthService';
import { useAbortSignal } from '../../../Components/State/useAbortSignal';
import { useSessionManager } from '../../../Components/Providers/Session';
import { LoadingSpinner } from '../../../Components/LoadingSpinner';

export const Card = styled(MuiCard)(({ theme }) => ({
    display: 'flex',
    flexDirection: 'column',
    alignSelf: 'center',
    width: '100%',
    padding: theme.spacing(4),
    gap: theme.spacing(2),
    margin: 'auto',
    [theme.breakpoints.up('sm')]: {
        width: '40rem',
    },
    boxShadow:
        'hsla(220, 30%, 5%, 0.05) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.05) 0px 15px 35px -5px',
    ...theme.applyStyles('dark', {
        boxShadow:
            'hsla(220, 30%, 5%, 0.5) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.08) 0px 15px 35px -5px',
    }),
}));

export default function SignIn() {
    const usernameOrEmailKey = 'usernameOrEmail';
    const passwordKey = 'password';
    const signal = useAbortSignal();
    const location = useLocation();
    const navigate = useNavigate();

    const sessionManager = useSessionManager();

    const { state, handleChange, handleSubmit } = useForm({
        initialValues: {
            [usernameOrEmailKey]: '',
            [passwordKey]: '',
        },
        onSubmit: async (values) => {
            const usernameOrEmail = values[usernameOrEmailKey];
            const password = values[passwordKey];

            const login = usernameOrEmail.includes('@')
                ? await AuthService.login(
                      password,
                      null,
                      usernameOrEmail,
                      signal,
                  )
                : await AuthService.login(
                      password,
                      usernameOrEmail,
                      null,
                      signal,
                  );

            if (login.isSuccess()) {
                sessionManager.setSession(login.getRight());
                navigate(location.state ? location.state.from : '/');
                return;
            }

            let error = login.getLeft();

            if (error.status === 400 || error.status === 401) {
                error = {
                    ...error,
                    detail: 'Invalid credentials. Please try again.',
                };
            }

            return error;
        },
    });

    return (
        <Card variant="outlined">
            <Typography
                component="h1"
                variant="h4"
                sx={{
                    width: '100%',
                    fontSize: 'clamp(2rem, 10vw, 2.15rem)',
                    marginBottom: '0.5rem',
                }}
            >
                Sign in
            </Typography>
            <Box
                component="form"
                onSubmit={handleSubmit}
                noValidate
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    width: '100%',
                    gap: 2,
                }}
            >
                <FormControl>
                    <TextField
                        sx={{ marginBottom: '0.5rem' }}
                        label="Username or Email"
                        id={usernameOrEmailKey}
                        type="text"
                        name={usernameOrEmailKey}
                        placeholder=""
                        onChange={handleChange}
                        autoFocus
                        required
                        fullWidth
                        variant="outlined"
                    />
                </FormControl>
                <FormControl>
                    <FormLabel htmlFor="password"></FormLabel>
                    <TextField
                        sx={{ marginBottom: '0.5rem' }}
                        label="Password"
                        name={passwordKey}
                        placeholder="••••••"
                        type="password"
                        id={passwordKey}
                        autoComplete="current-password"
                        onChange={handleChange}
                        required
                        fullWidth
                        variant="outlined"
                    />
                </FormControl>
                {state.type == 'loading' ? (
                    <LoadingSpinner text="Logging in" />
                ) : (
                    <Button
                        sx={{ marginTop: 2 }}
                        type="submit"
                        fullWidth
                        variant="contained"
                        disabled={Object.values(state.values).some(
                            (value) => value === '',
                        )}
                    >
                        Sign in
                    </Button>
                )}
                {state.error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {state.error.detail}
                    </Alert>
                )}
            </Box>
            <Divider>or</Divider>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Typography sx={{ textAlign: 'center' }}>
                    Don&apos;t have an account?{' '}
                    <Link
                        onClick={() => navigate('/sign-up')}
                        variant="body2"
                        sx={{ alignSelf: 'center' }}
                    >
                        Sign up
                    </Link>
                </Typography>
            </Box>
        </Card>
    );
}
