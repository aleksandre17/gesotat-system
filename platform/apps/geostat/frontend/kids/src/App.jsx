import React, { useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Header from "./components/Header";
import Main from "./components/Main";
import Footer from "./components/footer";
import ErrorBoundary from "./components/ErrorBoundary";
import GlossaryModal from "./components/GlossaryModal";
import MainIndicators from "./pages/sections/MainIndicators";
import Population from "./pages/sections/Population";
import HealthCare from "./pages/sections/HealthCare";
import Education from "./pages/sections/Education";
import SocialProtection from "./pages/sections/SocialProtection";
import LivingStandards from "./pages/sections/LivingStandards";
import Employment from "./pages/sections/Employment";
import ICT from "./pages/sections/ICT";
import Tourism from "./pages/sections/Tourism";
import Offences from "./pages/sections/Offences";
import NationalAccounts from "./pages/sections/NationalAccounts";
import Infographic from "./pages/sections/Infographic";
import Goals from "./pages/sections/Goals";
import "./App.scss";

function App() {
  const [language, setLanguage] = useState("GE");
  const [glossaryOpen, setGlossaryOpen] = useState(false);

  const pageWrapper = (Component) => (
    <div
      className="min-h-screen flex flex-col"
      style={{ backgroundColor: "var(--app-bg)", color: "var(--app-text)" }}
    >
      <div className="w-full flex justify-center" style={{ backgroundColor: "var(--app-bg)" }}>
        <div className="w-full">
          <Header language={language} setLanguage={setLanguage} onGlossaryOpen={() => setGlossaryOpen(true)} />
        </div>
      </div>
      {glossaryOpen && <GlossaryModal language={language} onClose={() => setGlossaryOpen(false)} />}
      <div className="w-full flex flex-1 justify-center" style={{ marginTop: 0 }}>
        <div className="w-full">
          <Component language={language} />
        </div>
      </div>
      <Footer className="mt-auto" language={language} setLanguage={setLanguage} onGlossaryOpen={() => setGlossaryOpen(true)} />
    </div>
  );

  return (
    <ErrorBoundary language={language}>
      <BrowserRouter>
        <Routes>
          <Route
            path="/"
            element={
              <div
                className="min-h-screen flex flex-col"
                style={{ backgroundColor: "var(--app-bg)", color: "var(--app-text)" }}
              >
                <div className="w-full flex justify-center" style={{ backgroundColor: "var(--app-bg)" }}>
                  <div className="w-full">
                    <Header language={language} setLanguage={setLanguage} onGlossaryOpen={() => setGlossaryOpen(true)} />
                  </div>
                </div>
                {glossaryOpen && <GlossaryModal language={language} onClose={() => setGlossaryOpen(false)} />}
                <div className="w-full flex flex-1 justify-center" style={{ marginTop: 0 }}>
                  <div className="w-full">
                    <Main language={language} setLanguage={setLanguage} />
                  </div>
                </div>
                <Footer className="mt-auto" language={language} setLanguage={setLanguage} onGlossaryOpen={() => setGlossaryOpen(true)} />
              </div>
            }
          />
          <Route path="/statistics/main-indicators" element={pageWrapper(MainIndicators)} />
          <Route path="/statistics/population" element={pageWrapper(Population)} />
          <Route path="/statistics/health-care" element={pageWrapper(HealthCare)} />
          <Route path="/statistics/education" element={pageWrapper(Education)} />
          <Route path="/statistics/social-protection" element={pageWrapper(SocialProtection)} />
          <Route path="/statistics/living-standards" element={pageWrapper(LivingStandards)} />
          <Route path="/statistics/employment" element={pageWrapper(Employment)} />
          <Route path="/statistics/ict" element={pageWrapper(ICT)} />
          <Route path="/statistics/tourism" element={pageWrapper(Tourism)} />
          <Route path="/statistics/offences" element={pageWrapper(Offences)} />
          <Route path="/statistics/national-accounts" element={pageWrapper(NationalAccounts)} />
          <Route path="/goals" element={pageWrapper(Goals)} />
          <Route path="/goals/:category" element={pageWrapper(Goals)} />
          <Route path="/infographic" element={pageWrapper(Infographic)} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
