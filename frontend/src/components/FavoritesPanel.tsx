import { loginHref } from "../api";
import type { Favorite } from "../types";
import { CITY_LABEL } from "../types";

export default function FavoritesPanel({
  open,
  items,
  selectedId,
  loggedIn,
  onSelect,
  onRemove,
  onClose,
}: {
  open: boolean;
  items: Favorite[];
  selectedId: number | null;
  loggedIn: boolean;
  onSelect: (item: Favorite) => void;
  onRemove: (locationId: number) => void;
  onClose: () => void;
}) {
  if (!open) {
    return null;
  }

  return (
    <aside className="panel favorites-panel">
      <div className="row">
        <h2>Избранное</h2>
        <button className="ghost" type="button" onClick={onClose}>
          Закрыть
        </button>
      </div>
      {!loggedIn && (
        <p className="hint">
          <a href={loginHref()}>Войдите</a>, чтобы сохранять площадки.
        </p>
      )}
      {loggedIn && items.length === 0 && (
        <p className="hint">Пока пусто. Нажмите «В избранное» на точке или запишитесь на слот — площадка появится здесь.</p>
      )}
      <div className="favorite-list">
        {items.map((item) => (
          <div
            key={item.id}
            className={`favorite-item ${selectedId === item.location.id ? "active" : ""}`}
          >
            <button className="favorite-main" type="button" onClick={() => onSelect(item)}>
              <strong>{item.location.name}</strong>
              <span className="meta">
                {CITY_LABEL[item.location.city]}
                {item.source === "PROMISE" ? " · вы записывались" : " · вручную"}
              </span>
            </button>
            <button
              className="ghost"
              type="button"
              onClick={() => onRemove(item.location.id)}
              aria-label="Убрать из избранного"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </aside>
  );
}
