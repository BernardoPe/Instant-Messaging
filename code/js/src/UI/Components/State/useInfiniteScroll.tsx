import { Pagination } from '../../../Domain/pagination/Pagination';
import { PaginationRequest } from '../../../Domain/pagination/PaginationRequest';
import { ApiResult, ProblemResponse } from '../../../Services/media/Problem';
import { Identifier } from '../../../Domain/wrappers/identifier/Identifier';
import { IdentifiableValue } from '../../../Domain/IdentifiableValue';
import { useEffect, useReducer } from 'react';

/**
 * Defines the parameters for the InfiniteScroll hook component.
 */
export interface InfiniteScrollProps<T> {
    fetchItemsRequest: (
        pageRequest: PaginationRequest,
        items?: IdentifiableValue<T>[],
    ) => ApiResult<Pagination<T>>;
    limit?: number;
    getCount?: boolean;
    useOffset?: boolean;
}

/**
 * Defines the InfiniteScroll hook component return type.
 */
export interface InfiniteScroll<T> {
    state: InfiniteScrollState<T>;
    loadMore: () => void;
    handleItemCreate?: (item: T) => void;
    handleItemUpdate?: (item: T) => void;
    handleItemDelete?: (itemId: Identifier) => void;
}

export type InfiniteScrollState<T> = {
    type: 'loading' | 'loaded' | 'error' | 'initial';
    paginationState: Pagination<T>;
    error: any;
    queuedUpdates: InfiniteScrollAction<T>[];
};

export type InfiniteScrollAction<T> =
    | {
          type: 'load' | 'loaded';
          payload?: Pagination<T>;
      }
    | {
          type: 'update' | 'create';
          payload: IdentifiableValue<T>;
      }
    | {
          type: 'delete';
          payload: Identifier;
      }
    | {
          type: 'error';
          payload: ProblemResponse;
      }
    | {
          type: 'clearUpdates';
      };

export function useInfiniteScroll<T>(
    props: InfiniteScrollProps<IdentifiableValue<T>>,
): InfiniteScroll<IdentifiableValue<T>> {
    function reducer(
        state: InfiniteScrollState<IdentifiableValue<T>>,
        action: InfiniteScrollAction<IdentifiableValue<T>>,
    ): InfiniteScrollState<IdentifiableValue<T>> {
        switch (state.type) {
            case 'loading':
                switch (action.type) {
                    case 'load':
                        // Prevent loading if already in 'loading' state
                        return state;

                    case 'loaded':
                        return {
                            ...state,
                            type: 'loaded',
                            paginationState: {
                                items: state.paginationState.items.concat(
                                    action.payload.items,
                                ),
                                info: action.payload.info,
                            },
                            error: null,
                        };

                    case 'error':
                        return {
                            ...state,
                            type: 'error',
                            error: action.payload,
                        };

                    case 'update':
                    case 'delete':
                        const updateId =
                            action.type === 'update'
                                ? action.payload.id
                                : (action.payload as Identifier);
                        if (
                            !state.paginationState.items.find(
                                (i) => i.id.value === updateId.value,
                            )
                        ) {
                            return {
                                ...state,
                                queuedUpdates: [...state.queuedUpdates, action],
                            };
                        }
                        return processUpdate(state, action);
                    default:
                        return state;
                }

            case 'loaded':
            case 'error':
                switch (action.type) {
                    case 'load':
                        if (
                            state.paginationState.info &&
                            !state.paginationState.info.next
                        ) {
                            // No more pages to load
                            return state;
                        }
                        return {
                            ...state,
                            type: 'loading',
                            error: null,
                        };

                    case 'create':
                        return {
                            ...state,
                            paginationState: {
                                ...state.paginationState,
                                items: [
                                    action.payload,
                                    ...state.paginationState.items,
                                ],
                            },
                        };

                    case 'update':
                    case 'delete':
                        return processUpdate(state, action);

                    case 'clearUpdates':
                        state.queuedUpdates.forEach((update) =>
                            processUpdate(state, update),
                        );
                        return {
                            ...state,
                            queuedUpdates: [],
                        };

                    case 'error':
                        return {
                            ...state,
                            type: 'error',
                            error: action.payload,
                        };

                    default:
                        return state;
                }

            case 'initial':
                switch (action.type) {
                    case 'load':
                        return {
                            ...state,
                            type: 'loading',
                            error: null,
                        };
                    default:
                        return state;
                }

            default:
                return state;
        }
    }

    function processUpdate(
        state: InfiniteScrollState<IdentifiableValue<T>>,
        action: InfiniteScrollAction<IdentifiableValue<T>>,
    ): InfiniteScrollState<IdentifiableValue<T>> {
        switch (action.type) {
            case 'update':
                return {
                    ...state,
                    paginationState: {
                        ...state.paginationState,
                        items: state.paginationState.items.map((i) =>
                            i.id.value === action.payload.id.value
                                ? action.payload
                                : i,
                        ),
                    },
                };
            case 'delete':
                return {
                    ...state,
                    paginationState: {
                        ...state.paginationState,
                        items: state.paginationState.items.filter(
                            (i) => i.id.value !== action.payload.value,
                        ),
                    },
                };
            default:
                return state;
        }
    }

    const [state, dispatch] = useReducer(reducer, {
        type: 'initial',
        paginationState: {
            items: [],
            info: null,
        },
        error: null,
        queuedUpdates: [],
    });

    const loadMore = () => {
        dispatch({ type: 'load' });
    };

    const handleItemCreate = (item: IdentifiableValue<T>) => {
        dispatch({ type: 'create', payload: item });
    };

    const handleItemUpdate = (item: IdentifiableValue<T>) => {
        dispatch({ type: 'update', payload: item });
    };

    const handleItemDelete = (itemId: Identifier) => {
        dispatch({ type: 'delete', payload: itemId });
    };

    useEffect(() => {
        if (state.type !== 'loading') {
            return;
        }

        const pageRequest: PaginationRequest = {
            offset: props.useOffset ? state.paginationState.items.length : null,
            limit: props.limit || 50,
            getCount: props.getCount || false,
        };

        props
            .fetchItemsRequest(pageRequest, state.paginationState.items)
            .then((result) => {
                if (result.isSuccess()) {
                    dispatch({ type: 'loaded', payload: result.getRight() });
                } else {
                    dispatch({ type: 'error', payload: result.getLeft() });
                }
            })
            .catch((error) => {
                dispatch({ type: 'error', payload: error });
            });
    }, [state]);

    useEffect(() => {
        if (state.type !== 'loading' && state.queuedUpdates.length > 0) {
            dispatch({ type: 'clearUpdates' });
        }
    }, [state]);

    return {
        state,
        loadMore,
        handleItemCreate,
        handleItemUpdate,
        handleItemDelete,
    };
}
