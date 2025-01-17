import { Navigate, Outlet, useNavigate } from 'react-router-dom';
import Stack from '@mui/material/Stack';
import { Window } from '../../../Components/Utils/Layouts/Window';
import { Routes } from '../../../../routes';
import { useInfiniteScrollContextChannels } from '../../../Providers/InfiniteScrollProvider';
import Box from '@mui/material/Box';
import React from 'react';
import { ChannelSettingsDashboard } from './Dashboard/ChannelSettingsDashboard';

export function ChannelSettingsWindow() {
    const navigate = useNavigate();
    const { state, selectedChannel } = useInfiniteScrollContextChannels();

    const channel = state.paginationState.items.find((channel) => channel.id.value === selectedChannel?.value);

    if (!channel) {
        return <Navigate to={Routes.HOME} />;
    }

    return (
        <Window sx={{ p: 0 }} onClose={() => navigate(Routes.HOME)} height={'80vh'}>
            <Stack
                direction={'row'}
                spacing={2}
                sx={(theme) => ({
                    height: '100%',
                    width: '60vw',
                    [theme.breakpoints.down('md')]: {
                        width: '100%',
                    },
                })}
            >
                <ChannelSettingsDashboard />
                <Box display={'flex'} width={'100%'} justifyContent={'center'} alignItems={'center'}>
                    <Outlet />
                </Box>
            </Stack>
        </Window>
    );
}
