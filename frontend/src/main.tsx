import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./auth";
import WakeGate from "./components/WakeGate";
import "./index.css";

const basename =
  import.meta.env.BASE_URL === "/" ? undefined : import.meta.env.BASE_URL.replace(/\/$/, "");

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter basename={basename}>
      <WakeGate>
        <AuthProvider>
          <App />
        </AuthProvider>
      </WakeGate>
    </BrowserRouter>
  </React.StrictMode>
);
