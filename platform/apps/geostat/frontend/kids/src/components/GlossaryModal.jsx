import React, { useEffect, useState, useCallback } from "react";

const modalStyles = `
  @keyframes glossaryFadeIn {
    from { opacity: 0; }
    to   { opacity: 1; }
  }
  @keyframes glossarySlideUp {
    from { opacity: 0; transform: translateY(40px) scale(0.97); }
    to   { opacity: 1; transform: translateY(0)   scale(1);    }
  }
`;

const GlossaryModal = ({ language, onClose }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");

  const lang = language === "EN" ? "en" : "ka";

  useEffect(() => {
    setLoading(true);
    setError(null);
    fetch(`${import.meta.env.VITE_API_URL}/api/glossary?lang=${lang}`)
      .then((res) => {
        if (!res.ok) throw new Error("Server error");
        return res.json();
      })
      .then((data) => {
        const sorted = [...data].sort((a, b) =>
          (a.text || "").localeCompare(b.text || "", "ka", { sensitivity: "base" })
        );
        setItems(sorted);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [lang]);

  const handleBackdrop = useCallback(
    (e) => {
      if (e.target === e.currentTarget) onClose();
    },
    [onClose],
  );

  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const filtered = items.filter((item) =>
    item.text?.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <>
      <style>{modalStyles}</style>
      <div
        onClick={handleBackdrop}
        style={{
          position: "fixed",
          inset: 0,
          background: "rgba(0,0,0,0.45)",
          zIndex: 1000,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "16px",
          animation: "glossaryFadeIn 0.2s ease",
        }}
      >
      <div
        style={{
          background: "#fff",
          borderRadius: "16px",
          width: "100%",
          maxWidth: "960px",
          maxHeight: "80vh",
          display: "flex",
          flexDirection: "column",
          boxShadow: "0 8px 40px rgba(0,0,0,0.18)",
          fontFamily: "FiraGO, sans-serif",
          overflow: "hidden",
          animation: "glossarySlideUp 0.25s cubic-bezier(0.34,1.56,0.64,1)",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "20px 24px 16px",
            background: "#005c97",
          }}
        >
          <h2
            style={{
              margin: 0,
              fontSize: "20px",
              fontWeight: "700",
              color: "#fff",
              fontFeatureSettings: '"case" on',
            }}
          >
            {language === "EN" ? "Glossary" : "გლოსარიუმი"}
          </h2>
          <button
            onClick={onClose}
            aria-label="Close"
            style={{
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#fff",
              fontSize: "22px",
              lineHeight: 1,
              padding: "4px 8px",
            }}
          >
            ✕
          </button>
        </div>

        {/* Search */}
        <div style={{ padding: "12px 24px", borderBottom: "1px solid #e5e9f0" }}>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={language === "EN" ? "Search…" : "ძიება…"}
            style={{
              width: "100%",
              padding: "8px 14px",
              borderRadius: "8px",
              border: "1px solid #c5cfe0",
              fontSize: "14px",
              fontFamily: "FiraGO, sans-serif",
              outline: "none",
              color: "#37496d",
              boxSizing: "border-box",
            }}
          />
        </div>

        {/* Body */}
        <div style={{ overflowY: "auto", padding: "8px 0" }}>
          {loading && (
            <p style={{ textAlign: "center", padding: "32px", color: "#37496d" }}>
              {language === "EN" ? "Loading…" : "იტვირთება…"}
            </p>
          )}
          {error && (
            <p style={{ textAlign: "center", padding: "32px", color: "#c0392b" }}>
              {error}
            </p>
          )}
          {!loading && !error && filtered.length === 0 && (
            <p style={{ textAlign: "center", padding: "32px", color: "#8ea4c8" }}>
              {language === "EN" ? "No results." : "შედეგი არ მოიძებნა."}
            </p>
          )}
          {!loading &&
            !error &&
            filtered.map((item) => (
              <div
                key={item.ID}
                style={{
                  padding: "12px 24px",
                  borderBottom: "1px solid #f0f3f8",
                  fontSize: "14px",
                  color: "#37496d",
                  lineHeight: "1.6",
                }}
              >
                {(() => {
                  const text = item.text || "";
                  const dashIdx = text.search(/\s[–-]\s/);
                  if (dashIdx === -1) return <strong>{text}</strong>;
                  return (
                    <>
                      <strong>{text.slice(0, dashIdx)}</strong>
                      {text.slice(dashIdx)}
                    </>
                  );
                })()}
              </div>
            ))}
        </div>
      </div>
      </div>
    </>
  );
};

export default GlossaryModal;
