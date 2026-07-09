import { useCallback, useEffect, useState } from 'react';

interface CatalogCrudApi<T, FormData> {
  list: () => Promise<T[]>;
  create: (data: FormData) => Promise<T>;
  update: (id: number, data: FormData) => Promise<T>;
  remove: (id: number) => Promise<void>;
}

interface WithId {
  id: number;
}

/**
 * Shared list/create/update/delete state machine for the catalog screens
 * (categories, brands, locations) — each backed by an identically-shaped
 * CRUD API, differing only in field set.
 */
export function useCatalogCrud<T extends WithId, FormData>(api: CatalogCrudApi<T, FormData>) {
  const [items, setItems] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.list());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido');
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    load();
  }, [load]);

  const create = async (data: FormData) => {
    setSaving(true);
    try {
      const created = await api.create(data);
      setItems(prev => [created, ...prev]);
    } finally {
      setSaving(false);
    }
  };

  const update = async (id: number, data: FormData) => {
    setSaving(true);
    try {
      const updated = await api.update(id, data);
      setItems(prev => prev.map(item => (item.id === id ? updated : item)));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id: number) => {
    await api.remove(id);
    setItems(prev => prev.filter(item => item.id !== id));
  };

  return { items, loading, error, saving, reload: load, create, update, remove };
}
