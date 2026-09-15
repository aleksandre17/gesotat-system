import React, { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import "../../styles/Goals.scss";
import goalCoverImg from "../../assets/images/goal-cover.png";

const getGoalImage = (category, language) => {
  const langSuffix = language === "EN" ? "en" : "ka";
  return new URL(`../../assets/images/goal-${category}-${langSuffix}.png`, import.meta.url).href;
};

const buildDownloadUrl = (pathValue) => {
  if (!pathValue) return null;

  if (/^https?:\/\//i.test(pathValue) || pathValue.startsWith("/")) {
    return pathValue;
  }

  if (pathValue.includes("/")) {
    return `/files/${pathValue}`;
  }

  return `/files/goals/${pathValue}`;
};

const Goals = ({ language }) => {
  const { category: categoryParam } = useParams();

  const parsedCategory = Number(categoryParam);
  const initialCategory = Number.isInteger(parsedCategory) && parsedCategory >= 1 && parsedCategory <= 17
    ? parsedCategory
    : null;

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [openCategories, setOpenCategories] = useState(() => (initialCategory ? { [initialCategory]: true } : {}));
  const [downloaded, setDownloaded] = useState({});
  const goalRefs = useRef({});

  useEffect(() => {
    const controller = new AbortController();

    const loadGoals = async () => {
      try {
        setLoading(true);
        setError("");

        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/goals`, { signal: controller.signal });
        if (!response.ok) {
          throw new Error(`Failed to load goals: ${response.status}`);
        }

        const data = await response.json();
        setRows(Array.isArray(data) ? data : []);
      } catch (err) {
        if (err.name !== "AbortError") {
          setError(language === "EN" ? "Could not load goals data." : "მიზნების მონაცემების ჩატვირთვა ვერ მოხერხდა.");
        }
      } finally {
        setLoading(false);
      }
    };

    loadGoals();

    return () => controller.abort();
  }, [language]);

  useEffect(() => {
    if (!initialCategory) return;
    setOpenCategories((prev) => ({ ...prev, [initialCategory]: true }));
  }, [initialCategory]);

  const groupedGoals = useMemo(() => {
    const map = new Map();

    rows.forEach((row) => {
      const category = Number(row.category);
      if (!Number.isInteger(category)) return;

      if (!map.has(category)) {
        map.set(category, {
          category,
          titleGE: row.category_title_geo || `მიზანი ${category}`,
          titleEN: row.category_title_eng || `Goal ${category}`,
          items: [],
        });
      }

      map.get(category).items.push({
        id: row.ID,
        titleGE: row.title_geo,
        titleEN: row.title_eng,
        pathGE: row.path_geo,
        pathEN: row.path_eng,
      });
    });

    return Array.from(map.values()).sort((a, b) => a.category - b.category);
  }, [rows]);

  useEffect(() => {
    if (!initialCategory || loading || groupedGoals.length === 0) return;

    const activeGoalElement = goalRefs.current[initialCategory];
    if (!activeGoalElement) return;

    activeGoalElement.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }, [groupedGoals, initialCategory, loading]);

  const toggleCategory = (category) => {
    setOpenCategories((prev) => ({ ...prev, [category]: !prev[category] }));
  };

  return (
    <div className="w-full max-w-screen-2xl mx-auto goals-page" style={{ paddingTop: "30px" }}>
      <div className="goals-hero">
        <img
          className="goals-hero-image"
          src={goalCoverImg}
          alt={language === "EN" ? "Sustainable Development Goals" : "მდგრადი განვითარების მიზნები"}
        />
        <div className="goals-hero-title">
          {language === "EN" ? <>SUSTAINABLE DEVELOPMENT <br />GOALS</> : <>მდგრადი განვითარების <br />მიზნები</>}
        </div>
      </div>

      <div className="px-4 pb-10">

      {loading && (
        <p style={{ textAlign: "center", color: "#37496d", fontFamily: "FiraGO" }}>
          {language === "EN" ? "Loading..." : "იტვირთება..."}
        </p>
      )}

      {error && (
        <p style={{ textAlign: "center", color: "#b42318", fontFamily: "FiraGO" }}>{error}</p>
      )}

      {!loading && !error && groupedGoals.length === 0 && (
        <p style={{ textAlign: "center", color: "#37496d", fontFamily: "FiraGO" }}>
          {language === "EN" ? "No data found." : "მონაცემები ვერ მოიძებნა."}
        </p>
      )}

      <div className="goals-container">
        {groupedGoals.map((goal) => {
          const isOpen = !!openCategories[goal.category];

          return (
            <div
              key={goal.category}
              className={`goal ${isOpen ? "active" : ""}`}
              ref={(element) => {
                if (element) {
                  goalRefs.current[goal.category] = element;
                }
              }}
            >
              <div className="goals-image">
                <img
                  src={getGoalImage(goal.category, language)}
                  alt={language === "EN" ? `Goal ${goal.category}` : `მიზანი ${goal.category}`}
                />
              </div>

              <div className="goals-title">
                {language === "EN" ? goal.titleEN : goal.titleGE}
              </div>

              <button
                type="button"
                className={`goals-show ${isOpen ? "open" : ""}`}
                onClick={() => toggleCategory(goal.category)}
                aria-expanded={isOpen}
                aria-label={language === "EN" ? "Toggle goal files" : "მიზნის ფაილების გახსნა"}
              >
                <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect width="50" height="52" rx="15" fill="#3B97D3" />
                  <path d="M33.3962 22H16.6038C16.1001 22 15.8188 22.4415 16.1308 22.743L24.527 30.8265C24.7673 31.0578 25.2301 31.0578 25.473 30.8265L33.8692 22.743C34.1812 22.4415 33.8999 22 33.3962 22Z" fill="white" />
                </svg>
              </button>

              <div className="goals-filelist">
                {goal.items.map((item) => {
                  const filePath = language === "EN" ? item.pathEN : item.pathGE;
                  const href = buildDownloadUrl(filePath);

                  return (
                    <div key={`${goal.category}-${item.id}`} className="goals-filelist-item">
                      <div className="image">
                        <svg width="35" height="33" viewBox="0 0 35 33" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                          <path d="M33.9062 28.4389H18.5937C17.99 28.4389 17.5 27.9489 17.5 27.3451C17.5 26.7414 17.5 6.07393 17.5 5.47018C17.5 4.86643 17.99 4.37643 18.5937 4.37643H33.9062C34.51 4.37643 35 4.86643 35 5.47018V27.3451C35 27.9489 34.51 28.4389 33.9062 28.4389Z" fill="#ECEFF1" />
                          <path d="M22.9687 10.939H18.5937C17.99 10.939 17.5 10.449 17.5 9.84524C17.5 9.24149 17.99 8.7515 18.5937 8.7515H22.9687C23.5725 8.7515 24.0625 9.24149 24.0625 9.84524C24.0625 10.449 23.5725 10.939 22.9687 10.939Z" fill="#388E3C" />
                          <path d="M22.9687 15.314H18.5937C17.99 15.314 17.5 14.824 17.5 14.2202C17.5 13.6165 17.99 13.1265 18.5937 13.1265H22.9687C23.5725 13.1265 24.0625 13.6165 24.0625 14.2202C24.0625 14.824 23.5725 15.314 22.9687 15.314Z" fill="#388E3C" />
                          <path d="M22.9687 19.689H18.5937C17.99 19.689 17.5 19.199 17.5 18.5952C17.5 17.9915 17.99 17.5015 18.5937 17.5015H22.9687C23.5725 17.5015 24.0625 17.9915 24.0625 18.5952C24.0625 19.199 23.5725 19.689 22.9687 19.689Z" fill="#388E3C" />
                          <path d="M22.9687 24.064H18.5937C17.99 24.064 17.5 23.574 17.5 22.9702C17.5 22.3665 17.99 21.8765 18.5937 21.8765H22.9687C23.5725 21.8765 24.0625 22.3665 24.0625 22.9702C24.0625 23.574 23.5725 24.064 22.9687 24.064Z" fill="#388E3C" />
                          <path d="M29.5308 10.939H27.3433C26.7395 10.939 26.2495 10.449 26.2495 9.84527C26.2495 9.24153 26.7395 8.75153 27.3433 8.75153H29.5308C30.1345 8.75153 30.6245 9.24153 30.6245 9.84527C30.6245 10.449 30.1345 10.939 29.5308 10.939Z" fill="#388E3C" />
                          <path d="M29.5308 15.314H27.3433C26.7395 15.314 26.2495 14.824 26.2495 14.2202C26.2495 13.6165 26.7395 13.1265 27.3433 13.1265H29.5308C30.1345 13.1265 30.6245 13.6165 30.6245 14.2202C30.6245 14.824 30.1345 15.314 29.5308 15.314Z" fill="#388E3C" />
                          <path d="M29.5308 19.689H27.3433C26.7395 19.689 26.2495 19.199 26.2495 18.5952C26.2495 17.9915 26.7395 17.5015 27.3433 17.5015H29.5308C30.1345 17.5015 30.6245 17.9915 30.6245 18.5952C30.6245 19.199 30.1345 19.689 29.5308 19.689Z" fill="#388E3C" />
                          <path d="M29.5308 24.064H27.3433C26.7395 24.064 26.2495 23.574 26.2495 22.9702C26.2495 22.3665 26.7395 21.8765 27.3433 21.8765H29.5308C30.1345 21.8765 30.6245 22.3665 30.6245 22.9702C30.6245 23.574 30.1345 24.064 29.5308 24.064Z" fill="#388E3C" />
                          <path d="M19.2915 0.253036C19.0422 0.0452238 18.7075 -0.0444636 18.3925 0.0211613L0.892499 3.30241C0.374062 3.39866 0 3.84928 0 4.37647V28.4389C0 28.9639 0.374062 29.4167 0.892499 29.513L18.3925 32.7942C18.4581 32.8074 18.5259 32.8139 18.5937 32.8139C18.8475 32.8139 19.0947 32.7264 19.2915 32.5624C19.5431 32.3545 19.6875 32.0439 19.6875 31.7202V1.09522C19.6875 0.769285 19.5431 0.460848 19.2915 0.253036Z" fill="#2E7D32" />
                          <path d="M15.0414 20.063L11.583 16.1102L15.0808 11.6127C15.4526 11.1358 15.3651 10.4489 14.8904 10.0771C14.4158 9.70519 13.7289 9.79269 13.3548 10.2674L10.1151 14.4324L7.38515 11.313C6.98483 10.8536 6.29358 10.8121 5.84296 11.2102C5.38796 11.6083 5.34202 12.2996 5.74015 12.7524L8.74796 16.1911L5.69859 20.1111C5.32671 20.588 5.41421 21.2749 5.8889 21.6467C6.09015 21.802 6.32858 21.8764 6.56265 21.8764C6.88858 21.8764 7.21015 21.732 7.42671 21.4542L10.2158 17.8667L13.3964 21.5002C13.6129 21.7495 13.9148 21.8764 14.2189 21.8764C14.4748 21.8764 14.7308 21.7867 14.9386 21.6052C15.3936 21.207 15.4395 20.5158 15.0414 20.063Z" fill="#FAFAFA" />
                        </svg>
                      </div>

                      <div className="title">
                        <span>{language === "EN" ? item.titleEN : item.titleGE}</span>
                      </div>

                      {href ? (
                        <a
                          href={href}
                          className="download"
                          target="_blank"
                          rel="noreferrer"
                          onClick={() => setDownloaded((prev) => ({ ...prev, [item.id]: true }))}
                        >
                          <div>{language === "EN" ? "Download" : "გადმოწერა"}</div>
                          <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg" className={`download-icon ${downloaded[item.id] ? "hidden" : ""}`}>
                            <rect width="50" height="52" rx="15" fill="#040571" />
                            <path d="M18.1426 33.7778H31.8574C32.147 33.7779 32.4258 33.8848 32.6373 34.077C32.8489 34.2692 32.9776 34.5323 32.9973 34.8132C33.0171 35.094 32.9264 35.3717 32.7437 35.5901C32.5609 35.8085 32.2997 35.9513 32.0129 35.9896L31.8574 36H18.1426C17.853 35.9999 17.5742 35.893 17.3627 35.7008C17.1511 35.5086 17.0224 35.2455 17.0027 34.9646C16.9829 34.6838 17.0736 34.4061 17.2563 34.1877C17.4391 33.9693 17.7003 33.8265 17.9871 33.7881L18.1426 33.7778ZM24.8446 16.0104L25 16C25.2762 16 25.543 16.0972 25.7512 16.2737C25.9593 16.4502 26.0947 16.694 26.1322 16.96L26.1429 17.1111V28.5007L29.5792 25.1615C29.773 24.9731 30.03 24.8588 30.3033 24.8392C30.5766 24.8196 30.848 24.8961 31.0681 25.0548L31.1961 25.1615C31.3898 25.3499 31.5075 25.5997 31.5276 25.8654C31.5477 26.131 31.469 26.3949 31.3058 26.6089L31.1961 26.7333L25.8077 31.9704C25.6141 32.1585 25.3576 32.273 25.0847 32.2928C24.8117 32.3127 24.5405 32.2366 24.3204 32.0785L24.1923 31.9704L18.8039 26.7333C18.5994 26.5351 18.4797 26.2689 18.4689 25.9883C18.458 25.7078 18.5568 25.4336 18.7454 25.221C18.934 25.0083 19.1984 24.873 19.4855 24.8422C19.7725 24.8114 20.061 24.8873 20.2927 25.0548L20.4208 25.1615L23.8571 28.5037V17.1111C23.8571 16.8426 23.9571 16.5832 24.1387 16.3808C24.3202 16.1785 24.5709 16.0469 24.8446 16.0104Z" fill="white" />
                          </svg>
                          <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg" className={`downloaded-icon ${downloaded[item.id] ? "visible" : ""}`}>
                            <rect width="50" height="52" rx="15" fill="#388E3C" />
                            <path d="M31.9318 21.3551C31.4588 20.8815 30.6907 20.8818 30.2171 21.3551L22.5 29.0725L19.0701 25.6426C18.5965 25.169 17.8288 25.169 17.3552 25.6426C16.8816 26.1162 16.8816 26.8839 17.3552 27.3575L21.6424 31.6447C21.8791 31.8814 22.1894 32 22.4997 32C22.8101 32 23.1207 31.8817 23.3573 31.6447L31.9318 23.07C32.4054 22.5967 32.4054 21.8287 31.9318 21.3551Z" fill="white" />
                          </svg>
                        </a>
                      ) : null}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
      </div>
    </div>
  );
};

export default Goals;