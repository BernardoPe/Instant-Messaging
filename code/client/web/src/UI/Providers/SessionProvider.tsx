import React, { createContext, useContext, useState } from 'react';
import { Session } from '../../Domain/sessions/Session';
import { ApiResult } from '../../Services/media/Problem';
import { useNavigate } from 'react-router-dom';
import { AuthService } from '../../Services/auth/AuthService';
import { AccessToken } from '../../Domain/tokens/AccessToken';
import { RefreshToken } from '../../Domain/tokens/RefreshToken';
import { Identifier } from '../../Domain/wrappers/identifier/Identifier';
import { Routes } from '../../routes';

const SESSION_STORAGE_KEY = 'session';

/**
 * Session manager
 *
 * @param session The current session.
 * @param setSession Sets the current session.
 * @param clearSession Clears the current session.
 */
export interface SessionManager {
    readonly session: Session | null;
    readonly setSession: (session: Session) => void;
    readonly clearSession: () => void;
    executeWithRefresh<T>(request: () => ApiResult<T> | void): ApiResult<T>;
}

const SessionContext = createContext<SessionManager>({
    session: null,
    setSession: () => {
        throw new Error('Not implemented');
    },
    clearSession: () => {
        throw new Error('Not implemented');
    },
    executeWithRefresh<T>(): ApiResult<T> {
        throw new Error('Not implemented');
    },
});

export default function SessionProvider({ children }: { children: React.ReactNode }) {
    const [session, setSession] = useState<Session | null>(() => {
        const sessionJson = localStorage.getItem(SESSION_STORAGE_KEY);
        if (sessionJson) {
            const session = JSON.parse(sessionJson);
            const user = session.user;
            const accessToken = new AccessToken(session.accessToken.token, new Date(session.accessToken.expiresAt));
            const refreshToken = new RefreshToken(session.refreshToken.token, new Date(session.refreshToken.expiresAt));
            return new Session(
                new Identifier(session.id),
                user,
                accessToken,
                refreshToken,
                new Date(session.expiresAt),
            );
        }
    });

    const [refreshPromise, setRefreshPromise] = useState<Promise<void> | null>(null);

    const navigate = useNavigate();

    const clearSession = () => {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        setSession(null);
    };

    async function executeWithRefresh<T>(request: () => ApiResult<T>, signal?: AbortSignal): ApiResult<T> {
        if (!session || new Date(session.expiresAt) < new Date()) {
            clearSession();
            navigate(Routes.SIGN_IN);
            return;
        }

        if (session.accessToken.isExpired()) {
            if (!refreshPromise) {
                const newRefreshPromise = refreshSession(signal);
                setRefreshPromise(newRefreshPromise);
                try {
                    await newRefreshPromise;
                } finally {
                    setRefreshPromise(null);
                }
            } else {
                await refreshPromise;
            }
        }

        const afterRefresh = await request();

        if (afterRefresh && afterRefresh.isFailure() && afterRefresh.getLeft().status === 401) {
            clearSession();
            navigate(Routes.SIGN_IN);
            return;
        }

        return afterRefresh;
    }

    const refreshSession = async (signal?: AbortSignal) => {
        const result = await AuthService.refresh(signal);
        if (result.isSuccess()) {
            updateSession(result.getRight());
        } else {
            if (result.getLeft().status === 401) {
                clearSession();
                navigate(Routes.SIGN_IN);
                return;
            }
        }
    };

    const updateSession = (session: Session) => {
        localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
        setSession(session);
    };

    return (
        <SessionContext.Provider
            value={{
                session,
                setSession: updateSession,
                clearSession,
                executeWithRefresh,
            }}
        >
            {children}
        </SessionContext.Provider>
    );
}

export function useSessionManager(): SessionManager {
    return useContext(SessionContext);
}

export function useLoggedIn(): boolean {
    const session = useSessionManager().session;
    if (!session) {
        return false;
    }
    return new Date(session.expiresAt) > new Date();
}
