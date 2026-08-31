import { loginHref } from "../api";
import { useI18n } from "../i18n";
import type { Favorite } from "../types";

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
  const { t, cityLabel } = useI18n();

  if (!open) {
    return null;
  }

  return (
    <aside className="panel favorites-panel">
      <div className="row">
        <h2>{t("favorites")}</h2>
        <button className="ghost" type="button" onClick={onClose}>
          {t("close")}
        </button>
      </div>
      {!loggedIn && (
        <p className="hint">
          <a href={loginHref()}>{t("signIn")}</a>
          {t("favoritesLoginRest")}
        </p>
      )}
      {loggedIn && items.length === 0 && <p className="hint">{t("favoritesEmpty")}</p>}
      <div className="favorite-list">
        {items.map((item) => (
          <div
            key={item.id}
            className={`favorite-item ${selectedId === item.location.id ? "active" : ""}`}
          >
            <button className="favorite-main" type="button" onClick={() => onSelect(item)}>
              <strong>{item.location.name}</strong>
              <span className="meta">
                {cityLabel(item.location.city)}
                {item.source === "PROMISE" ? ` · ${t("favoritedViaPromise")}` : ` · ${t("favoritedManual")}`}
              </span>
            </button>
            <button
              className="ghost"
              type="button"
              onClick={() => onRemove(item.location.id)}
              aria-label={t("removeFavorite")}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </aside>
  );
}
