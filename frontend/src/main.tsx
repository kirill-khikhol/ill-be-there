import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./auth";
import WakeGate from "./components/WakeGate";
import { I18nProvider } from "./i18n";
import "./index.css";

const basename =
  import.meta.env.BASE_URL === "/" ? undefined : import.meta.env.BASE_URL.replace(/\/$/, "");

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <I18nProvider>
      <BrowserRouter basename={basename}>
        <WakeGate>
          <AuthProvider>
            <App />
          </AuthProvider>
        </WakeGate>
      </BrowserRouter>
    </I18nProvider>
  </React.StrictMode>
);
