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
import LangSwitch from "../components/LangSwitch";
import { useI18n } from "../i18n";
import type { City, Favorite, Place } from "../types";

L.Icon.Default.mergeOptions({
  iconUrl: markerIcon,
  iconRetinaUrl: markerRetina,
  shadowUrl: markerShadow,
});

const CITY_FILTERS: Array<{ id: City | "ALL"; center?: [number, number]; zoom?: number }> = [
  { id: "ALL", center: [32.35, 34.92], zoom: 9 },
  { id: "HAIFA", center: [32.81, 35.0], zoom: 13 },
  { id: "TEL_AVIV", center: [32.08, 34.78], zoom: 13 },
  { id: "RAMAT_GAN", center: [32.08, 34.82], zoom: 14 },
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
  const { t, cityLabel, translateError } = useI18n();
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
      setError(err instanceof Error ? err.message : "errorLoadPlaces");
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
      setError(err instanceof Error ? err.message : "errorAddPlace");
    }
  };

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand-row">
          <div className="brand">
            <strong>I'll Be There</strong>
            <span>{t("tagline")}</span>
          </div>
          <LangSwitch compact />
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
              {item.id === "ALL" ? t("cityAll") : cityLabel(item.id)}
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
                {t("favorites")}
                {favorites.length > 0 ? ` (${favorites.length})` : ""}
              </button>
              <button className="ghost" type="button" onClick={logout}>
                {t("logOut")}
              </button>
            </>
          ) : googleEnabled ? (
            <a className="btn" href={loginHref()}>
              {t("signInGoogle")}
            </a>
          ) : (
            <span className="hint">{t("googleNotConfigured")}</span>
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
            {adding ? t("cancel") : t("addPin")}
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
        {loading && <div className="add-banner">{t("loadingOsm")}</div>}
        {oauthError && (
          <div className="add-banner">
            <span className="error">
              {t("oauthPrefix", { detail: translateError(oauthError, "errorGeneric") })}
            </span>
            <button
              className="ghost"
              type="button"
              onClick={() => {
                searchParams.delete("oauthError");
                setSearchParams(searchParams, { replace: true });
              }}
            >
              {t("close")}
            </button>
          </div>
        )}
        {error && !loading && <div className="add-banner">{translateError(error, "errorLoadPlaces")}</div>}
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
                  placeholder={t("placeNamePlaceholder")}
                  required
                />
                <button className="btn" type="submit">
                  {t("save")}
                </button>
              </form>
            ) : (
              t("clickMap")
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
                  setError(err instanceof Error ? err.message : "errorUpdateFavorite");
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
                setError(err instanceof Error ? err.message : "errorRemoveFavorite");
              }
            })();
          }}
        />
      </div>
    </div>
  );
}
