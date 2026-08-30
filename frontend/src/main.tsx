import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./auth";
import WakeGate from "./components/WakeGate";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <WakeGate>
        <AuthProvider>
          <App />
        </AuthProvider>
      </WakeGate>
    </BrowserRouter>
  </React.StrictMode>
);
