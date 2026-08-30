import L from "leaflet";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";
import markerRetina from "leaflet/dist/images/marker-icon-2x.png";
import "leaflet/dist/leaflet.css";
import { useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from "react-leaflet";
import { useSearchParams } from "react-router-dom";
import { api, loginHref } from "../api";
import { useAuth } from "../auth";
import LocationPanel from "../components/LocationPanel";
import FavoritesPanel from "../components/FavoritesPanel";
import type { City, Favorite, Place } from "../types";

L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerRetina,
  shadowUrl: markerShadow,
});

const CITY_FILTERS: Array<{ id: City | "ALL"; label: string; center?: [number, number]; zoom?: number }> = [
  { id: "ALL", label: "Все", center: [32.35, 34.92], zoom: 9 },
  { id: "HAIFA", label: "Хайфа", center: [32.81, 35.0], zoom: 13 },
  { id: "TEL_AVIV", label: "Тель-Авив", center: [32.08, 34.78], zoom: 13 },
  { id: "RAMAT_GAN", label: "Рамат-Ган", center: [32.08, 34.82], zoom: 14 },
];

function Recenter({ center, zoom, nonce }: { center: [number, number]; zoom: number; nonce: number }) {
  const map = useMap();
  useEffect(() => {
    if (nonce === 0) {
      map.setView(center, zoom);
      return;
    }
    map.flyTo(center, zoom, { duration: 0.7 });
  }, [center, map, nonce, zoom]);
  return null;
}

function MapClick({
  enabled,
  onPick,
}: {
  enabled: boolean;
  onPick: (lat: number, lng: number) => void;
}) {
  useMapEvents({
    click(event) {
      if (enabled) {
        onPick(event.latlng.lat, event.latlng.lng);
      }
    },
  });
  return null;
}

export default function MapPage() {
  const { user, googleEnabled, logout } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const oauthError = searchParams.get("oauthError");
  const [city, setCity] = useState<City | "ALL">("ALL");
  const [places, setPlaces] = useState<Place[]>([]);
  const [selected, setSelected] = useState<Place | null>(null);
  const [adding, setAdding] = useState(false);
  const [draft, setDraft] = useState<{ lat: number; lng: number } | null>(null);
  const [draftName, setDraftName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [favorites, setFavorites] = useState<Favorite[]>([]);
  const [favoritesOpen, setFavoritesOpen] = useState(false);
  const [mapFocus, setMapFocus] = useState<{ center: [number, number]; zoom: number; nonce: number }>({
    center: [32.35, 34.92],
    zoom: 9,
    nonce: 0,
  });

  const view = useMemo(() => CITY_FILTERS.find((item) => item.id === city) ?? CITY_FILTERS[0], [city]);
  const favoriteIds = useMemo(() => new Set(favorites.map((item) => item.location.id)), [favorites]);

  const loadFavorites = async () => {
    if (!user) {
      setFavorites([]);
      return;
    }
    try {
      setFavorites(await api.favorites());
    } catch {
      setFavorites([]);
    }
  };

  const loadPlaces = async (nextCity: City | "ALL") => {
    setLoading(true);
    setError(null);
    try {
      setPlaces(await api.locations(nextCity));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось загрузить точки");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadPlaces(city);
  }, [city]);

  useEffect(() => {
    void loadFavorites();
  }, [user]);

  const submitPlace = async () => {
    if (!draft || !draftName.trim()) {
      return;
    }
    try {
      const created = await api.createLocation({
        name: draftName.trim(),
        latitude: draft.lat,
        longitude: draft.lng,
      });
      setPlaces((current) => [...current, created]);
      setSelected(created);
      setAdding(false);
      setDraft(null);
      setDraftName("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Не удалось добавить точку");
    }
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <strong>I'll Be There</strong>
          <span>Обещай прийти — остальные увидят</span>
        </div>
        <div className="filters">
          {CITY_FILTERS.map((item) => (
            <button
              key={item.id}
              className={`chip ${city === item.id ? "active" : ""}`}
              type="button"
              onClick={() => {
                setCity(item.id);
                setMapFocus({
                  center: item.center ?? [32.35, 34.92],
                  zoom: item.zoom ?? 9,
                  nonce: Date.now(),
                });
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="userbox">
          {user ? (
            <>
              {user.avatarUrl && <img src={user.avatarUrl} alt="" />}
              <span>{user.name}</span>
              <button
                className={`ghost ${favoritesOpen ? "active" : ""}`}
                type="button"
                onClick={() => {
                  if (!user) {
                    window.location.href = loginHref();
                    return;
                  }
                  setFavoritesOpen((value) => !value);
                }}
              >
                Избранное{favorites.length > 0 ? ` (${favorites.length})` : ""}
              </button>
              <button className="ghost" type="button" onClick={logout}>
                Выйти
              </button>
            </>
          ) : googleEnabled ? (
            <a className="btn" href={loginHref()}>
              Войти через Google
            </a>
          ) : (
            <span className="hint">Google OAuth не настроен</span>
          )}
          <button
            className="ghost"
            type="button"
            onClick={() => {
              if (!user) {
                window.location.href = loginHref();
                return;
              }
              setAdding((value) => !value);
              setDraft(null);
            }}
          >
            {adding ? "Отмена" : "Добавить точку"}
          </button>
        </div>
      </header>
      <div className="map-wrap">
        <MapContainer center={view.center ?? [32.35, 34.92]} zoom={view.zoom ?? 9} scrollWheelZoom>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Recenter center={mapFocus.center} zoom={mapFocus.zoom} nonce={mapFocus.nonce} />
          <MapClick
            enabled={adding}
            onPick={(lat, lng) => {
              setDraft({ lat, lng });
            }}
          />
          {places.map((place) => (
            <Marker
              key={place.id}
              position={[place.latitude, place.longitude]}
              eventHandlers={{ click: () => setSelected(place) }}
            />
          ))}
          {draft && <Marker position={[draft.lat, draft.lng]} />}
        </MapContainer>
        {loading && (
          <div className="add-banner">Загружаем площадки из OpenStreetMap. Первый раз это может занять минуту.</div>
        )}
        {oauthError && (
          <div className="add-banner">
            <span className="error">Google OAuth: {oauthError}</span>
            <button
              className="ghost"
              type="button"
              onClick={() => {
                searchParams.delete("oauthError");
                setSearchParams(searchParams, { replace: true });
              }}
            >
              Закрыть
            </button>
          </div>
        )}
        {error && !loading && <div className="add-banner">{error}</div>}
        {adding && (
          <div className="add-banner">
            {draft ? (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  void submitPlace();
                }}
              >
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  placeholder="Название площадки"
                  required
                />
                <button className="btn" type="submit">
                  Сохранить
                </button>
              </form>
            ) : (
              "Кликните по карте, чтобы поставить точку"
            )}
          </div>
        )}
        {selected && (
          <LocationPanel
            place={selected}
            onClose={() => setSelected(null)}
            favorited={favoriteIds.has(selected.id)}
            onToggleFavorite={() => {
              void (async () => {
                try {
                  if (favoriteIds.has(selected.id)) {
                    await api.removeFavorite(selected.id);
                  } else {
                    await api.addFavorite(selected.id);
                  }
                  await loadFavorites();
                } catch (err) {
                  setError(err instanceof Error ? err.message : "Не удалось обновить избранное");
                }
              })();
            }}
            onPromiseCreated={() => void loadFavorites()}
          />
        )}
        <FavoritesPanel
          open={favoritesOpen}
          items={favorites}
          selectedId={selected?.id ?? null}
          loggedIn={Boolean(user)}
          onClose={() => setFavoritesOpen(false)}
          onSelect={(item) => {
            setSelected(item.location);
            setMapFocus({
              center: [item.location.latitude, item.location.longitude],
              zoom: 16,
              nonce: Date.now(),
            });
          }}
          onRemove={(locationId) => {
            void (async () => {
              try {
                await api.removeFavorite(locationId);
                await loadFavorites();
                if (selected?.id === locationId) {
                  // keep panel open; star updates via favorites reload
                }
              } catch (err) {
                setError(err instanceof Error ? err.message : "Не удалось убрать из избранного");
              }
            })();
          }}
        />
      </div>
    </div>
  );
}
