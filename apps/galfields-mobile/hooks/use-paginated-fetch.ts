import { useCallback, useEffect, useRef, useState } from 'react';
import type { Page } from '@/services/reports-api';

/**
 * Generic page-0-replace / page-N-append pagination, mirroring
 * contexts/products-context.tsx's `loadPage` pattern, for report screens
 * with no shared context to live in (each report's list is local to its
 * own screen). `fetchPage` is read through a ref on every call instead of
 * being a hook dependency, so passing a fresh inline closure each render
 * (e.g. capturing a filter value) doesn't retrigger the initial load loop.
 */
export function usePaginatedFetch<T>(fetchPage: (page: number, size: number) => Promise<Page<T>>, size = 20) {
  const fetchRef = useRef(fetchPage);
  fetchRef.current = fetchPage;

  const [items, setItems] = useState<T[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(
    async (pageToLoad: number, replace: boolean) => {
      (pageToLoad === 0 ? setLoading : setLoadingMore)(true);
      setError(null);
      try {
        const result = await fetchRef.current(pageToLoad, size);
        setItems(prev => (replace ? result.content : [...prev, ...result.content]));
        setPage(result.number);
        setTotalPages(result.totalPages);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'No se pudo cargar la información.');
      } finally {
        (pageToLoad === 0 ? setLoading : setLoadingMore)(false);
      }
    },
    [size],
  );

  useEffect(() => {
    loadPage(0, true);
  }, [loadPage]);

  const refresh = () => loadPage(0, true);
  const loadMore = () => {
    if (loading || loadingMore || page + 1 >= totalPages) return;
    loadPage(page + 1, false);
  };

  return { items, loading, loadingMore, error, refresh, loadMore, hasMore: page + 1 < totalPages };
}
