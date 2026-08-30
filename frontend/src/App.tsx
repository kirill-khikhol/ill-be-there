import { Route, Routes } from "react-router-dom";
import AuthCallback from "./pages/AuthCallback";
import MapPage from "./pages/MapPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MapPage />} />
      <Route path="/auth/callback" element={<AuthCallback />} />
    </Routes>
  );
}
