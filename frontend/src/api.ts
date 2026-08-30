import type { AuthConfig, City, DaySlots, Favorite, Place, SlotDetails, UserProfile } from "./types";

function apiBase(): string {
  const raw = import.meta.env.VITE_API_URL;
  if (!raw) {
    return "";
  }
  if (raw.startsWith("http://") || raw.startsWith("https://")) {
    return raw.replace(/\/$/, "");
  }
  return `https://${raw.replace(/\/$/, "")}`;
}

export const API_BASE = apiBase();

function token(): string | null {
  return localStorage.getItem("ibt_token");
}

export function setToken(value: string | null): void {
  if (value) {
    localStorage.setItem("ibt_token", value);
  } else {
    localStorage.removeItem("ibt_token");
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const jwt = token();
  if (jwt) {
    headers.set("Authorization", `Bearer ${jwt}`);
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message = data && data.error ? data.error : `Ошибка ${response.status}`;
    throw new Error(message);
  }
  return data as T;
}

export async function waitForHealth(onAttempt: (n: number) => void): Promise<void> {
  const maxAttempts = 40;
  for (let i = 1; i <= maxAttempts; i += 1) {
    onAttempt(i);
    try {
      const response = await fetch(`${API_BASE}/actuator/health`, { cache: "no-store" });
      if (response.ok) {
        return;
      }
    } catch {
      // still waking
    }
    await new Promise((resolve) => setTimeout(resolve, 3000));
  }
  throw new Error("Сервер не проснулся. Обновите страницу через минуту.");
}

export function loginHref(): string {
  const base = API_BASE || "http://localhost:8080";
  return `${base}/oauth2/authorization/google`;
}

export const api = {
  authConfig: () => apiFetch<AuthConfig>("/api/auth/config"),
  me: () => apiFetch<UserProfile>("/api/me"),
  locations: (city?: City | "ALL") => {
    const query = city && city !== "ALL" ? `?city=${city}` : "";
    return apiFetch<Place[]>(`/api/locations${query}`);
  },
  createLocation: (body: { name: string; latitude: number; longitude: number }) =>
    apiFetch<Place>("/api/locations", { method: "POST", body: JSON.stringify(body) }),
  daySlots: (locationId: number, date: string) =>
    apiFetch<DaySlots>(`/api/locations/${locationId}/promises?date=${date}`),
  slotDetails: (locationId: number, date: string, slot: string) =>
    apiFetch<SlotDetails>(
      `/api/locations/${locationId}/promises/details?date=${date}&slot=${encodeURIComponent(slot)}`
    ),
  createPromise: (locationId: number, date: string, slot: string) =>
    apiFetch<{ id: number }>("/api/promises", {
      method: "POST",
      body: JSON.stringify({ locationId, date, slot }),
    }),
  cancelPromise: (id: number) => apiFetch<void>(`/api/promises/${id}`, { method: "DELETE" }),
  myPromises: () => apiFetch<Array<{ id: number; locationId: number; date: string; slot: string }>>("/api/promises/mine"),
  favorites: () => apiFetch<Favorite[]>("/api/favorites"),
  addFavorite: (locationId: number) =>
    apiFetch<Favorite>("/api/favorites", { method: "POST", body: JSON.stringify({ locationId }) }),
  removeFavorite: (locationId: number) => apiFetch<void>(`/api/favorites/${locationId}`, { method: "DELETE" }),
};
