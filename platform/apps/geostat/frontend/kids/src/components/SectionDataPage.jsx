import React, { useEffect, useMemo, useState } from "react";
import Highcharts from "highcharts";
import HighchartsReactModule from "highcharts-react-official";
import geostatLogo from "../assets/images/geostatlogo.svg";
import eduLogo from "../assets/images/edu.svg";
import logoS4Ge from "../assets/images/logo-s4.png";
import logoS4En from "../assets/images/logo-s4-en.png";
import logoS5 from "../assets/images/logo-s5.svg";
import downloadIcon from "../assets/images/google-docs.png";
import barchartIcon from "../assets/images/barchart.png";
import linechartIcon from "../assets/images/linechart.png";
import "../styles/SectionDataPage.scss";

const HighchartsReact =
  HighchartsReactModule?.default ||
  HighchartsReactModule?.HighchartsReact ||
  HighchartsReactModule;

const defaultFilterData = [
  {
    key: "multi",
    categories: [1, 4],
    imageKey: "geostat",
    imgWidth: 120,
    labelGE: "საქსტატი",
    labelEN: "Geostat",
  },
  {
    key: "filter2",
    categories: [2],
    imageKey: "edu",
    imgWidth: 100,
    labelGE: "ადმნისისტრაციული წყაროები",
    labelEN: "Administrative Sources",
  },
  {
    key: "filter3",
    categories: [3],
    imageKey: "s4",
    imgWidth: 55,
    labelGE: "მოსახლეობის 2014 წლის საყოველთაო აღწერა",
    labelEN: "2014 GENERAL POPULATION CENSUS",
  },
  {
    key: "filter4",
    categories: [4],
    imageKey: "s5",
    imgWidth: 120,
    labelGE: "მრავალინდიკატორული კლასტერული კვლევა (MICS) 2018",
    labelEN: "Multiple Indicator Cluster Survey",
  },
];

const imageMap = {
  geostat: geostatLogo,
  edu: eduLogo,
  s5: logoS5,
};

const resolveFilterImage = (imageKey, language) => {
  if (imageKey === "s4") {
    return language === "EN" ? logoS4En : logoS4Ge;
  }
  return imageMap[imageKey] || geostatLogo;
};

const mainStatFolderMap = {
  population: "mosaxleoba",
  "health-care": "jandacva",
  education: "education",
  "social-protection": "social",
  "living-standards": "cxovrebisdone",
  employment: "labour",
  ict: "ICT",
  tourism: "turizmi",
  offences: "crime",
  "national-accounts": "mshp",
};

const buildSectionFileUrl = (sectionKey, pathValue) => {
  if (!pathValue) return null;

  if (/^https?:\/\//i.test(pathValue) || pathValue.startsWith("/")) {
    return pathValue;
  }

  const normalizedPath = pathValue.replace(/^files\//, "");

  if (normalizedPath.includes("/")) {
    return `/files/mainstat/${normalizedPath}`;
  }

  const folderName = mainStatFolderMap[sectionKey];
  if (!folderName) {
    return `/files/${normalizedPath}`;
  }

  return `/files/mainstat/${folderName}/${normalizedPath}`;
};

const CHART_SERIES_COLORS = ["#0066e0", "#00a3a3", "#f97316", "#7c3aed"];

const getFileSeriesNames = (file) => {
  const names = [
    ...(file?.chartdata?.barSeries || []).map((seriesItem) => seriesItem.name),
    ...(file?.chartdata?.lineSeries || []).map((seriesItem) => seriesItem.name),
  ];

  return [...new Set(names)];
};

const getSeriesColorByName = (file, seriesName) => {
  const orderedSeriesNames = getFileSeriesNames(file);
  const index = orderedSeriesNames.indexOf(seriesName);

  if (index === -1) {
    return CHART_SERIES_COLORS[0];
  }

  return CHART_SERIES_COLORS[index % CHART_SERIES_COLORS.length];
};

const parseChartData = (rawChartData) => {
  if (!rawChartData) return null;

  const stripTrailingCommas = (value) => String(value).replace(/,\s*([}\]])/g, "$1");

  const parsedValue = typeof rawChartData === "string"
    ? (() => {
        const base = rawChartData.trim();
        const candidates = [
          base,
          stripTrailingCommas(base),
          // Some DB rows store JSON with escaped newlines/tabs (e.g. "\\n").
          base.replace(/\\r\\n|\\n|\\r/g, " ").replace(/\\t/g, " "),
          stripTrailingCommas(base.replace(/\\r\\n|\\n|\\r/g, " ").replace(/\\t/g, " ")),
          base.replace(/\\"/g, '"'),
          stripTrailingCommas(base.replace(/\\"/g, '"')),
          base.replace(/\\"/g, '"').replace(/\\r\\n|\\n|\\r/g, " ").replace(/\\t/g, " "),
          stripTrailingCommas(base.replace(/\\"/g, '"').replace(/\\r\\n|\\n|\\r/g, " ").replace(/\\t/g, " ")),
        ];

        for (const candidate of candidates) {
          try {
            const once = JSON.parse(candidate);
            if (typeof once === "string") {
              try {
                return JSON.parse(once);
              } catch {
                return null;
              }
            }
            return once;
          } catch {
            // Try next candidate.
          }
        }

        return null;
      })()
    : rawChartData;

  if (!parsedValue || typeof parsedValue !== "object") {
    return null;
  }

  if (Array.isArray(parsedValue)) {
    if (parsedValue.length === 0 || typeof parsedValue[0] !== "object") {
      return null;
    }

    const categoryKey = Object.prototype.hasOwnProperty.call(parsedValue[0], "year") ? "year" : "label";
    const seriesKeys = Object.keys(parsedValue[0]).filter((key) => key !== categoryKey);

    if (seriesKeys.length === 0) {
      return null;
    }

    return {
      labels: parsedValue.map((entry) => String(entry[categoryKey] ?? "")),
      barSeries: seriesKeys.map((key) => ({
        name: key.trim() || "Value",
        data: parsedValue.map((entry) => Number(entry[key]) || 0),
      })),
      lineSeries: seriesKeys.map((key) => ({
        name: key.trim() || "Value",
        data: parsedValue.map((entry) => Number(entry[key]) || 0),
      })),
    };
  }

  if (!Array.isArray(parsedValue.labels)) {
    return null;
  }

  const bar = Array.isArray(parsedValue.bar) ? parsedValue.bar : [];
  const line = Array.isArray(parsedValue.line) ? parsedValue.line : bar;

  return {
    labels: parsedValue.labels,
    barSeries: [{ name: parsedValue.barLabel || "Value", data: bar }],
    lineSeries: [{ name: parsedValue.lineLabel || parsedValue.barLabel || "Value", data: line }],
  };
};

const parseSubCategories = (rawSubCategory) => {
  if (rawSubCategory === null || rawSubCategory === undefined) {
    return [];
  }

  return String(rawSubCategory)
    .split(",")
    .map((value) => Number(value.trim()))
    .filter(Number.isInteger);
};

const formatChartLabel = (value) => {
  const normalizedValue = String(value || "")
    .replace(/_/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (!normalizedValue) {
    return "";
  }

  return normalizedValue.replace(/\b[A-Za-z][A-Za-z-]*\b/g, (word) => {
    if (word === word.toUpperCase()) {
      return word;
    }

    return word.charAt(0).toUpperCase() + word.slice(1);
  });
};

const createDefaultFilesData = ({ sectionKey, titleGE, titleEN }) => [
  {
    id: 1,
    categories: [1],
    titleGE: `${titleGE} - ძირითადი მაჩვენებლები, 2025`,
    titleEN: `${titleEN} - Main Indicators, 2025`,
    href: `/files/indicators/${sectionKey}-main.pdf`,
    chartdata: {
      labels: ["2022", "2023", "2024", "2025"],
      bar: [120, 140, 165, 171],
      line: [118, 130, 160, 171],
    },
  },
  {
    id: 2,
    categories: [2],
    titleGE: `${titleGE} - თემატური მიმოხილვა`,
    titleEN: `${titleEN} - Thematic Overview`,
    href: `/files/indicators/${sectionKey}-overview.pdf`,
  },
  {
    id: 3,
    categories: [3],
    titleGE: `${titleGE} - დეტალური ანგარიში`,
    titleEN: `${titleEN} - Detailed Report`,
    href: `/files/indicators/${sectionKey}-detailed.pdf`,
    chartdata: {
      labels: ["2022", "2023", "2024", "2025"],
      bar: [90, 110, 133, 149],
      line: [85, 108, 129, 149],
    },
  },
  {
    id: 4,
    categories: [4],
    titleGE: `${titleGE} - რეგიონული ტენდენციები`,
    titleEN: `${titleEN} - Regional Trends`,
    href: `/files/indicators/${sectionKey}-regional.pdf`,
  },
];

const SectionDataPage = ({
  language,
  slideImg,
  titleGE,
  titleEN,
  coverPdfNumber,
  sectionKey,
  filterData = defaultFilterData,
  filesData,
}) => {
  const [apiFilesData, setApiFilesData] = useState(null);
  const [filesError, setFilesError] = useState("");
  const [filesLoading, setFilesLoading] = useState(!filesData);

  useEffect(() => {
    if (filesData) {
      setApiFilesData(filesData);
      setFilesLoading(false);
      setFilesError("");
      return undefined;
    }

    const controller = new AbortController();

    const loadFiles = async () => {
      try {
        setFilesLoading(true);
        setFilesError("");

        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/files?category=${coverPdfNumber}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Failed to load files: ${response.status}`);
        }

        const data = await response.json();
        const normalizedData = Array.isArray(data)
          ? data.map((file) => ({
              id: file.ID,
              categories: parseSubCategories(file.sub_category),
              titleGE: file.title_geo,
              titleEN: file.title_eng,
              hrefGE: buildSectionFileUrl(sectionKey, file.path_geo),
              hrefEN: buildSectionFileUrl(sectionKey, file.path_eng),
              chartdata: parseChartData(file.chartdata),
            }))
          : [];

        setApiFilesData(normalizedData);
      } catch (err) {
        if (err.name !== "AbortError") {
          setFilesError(language === "EN" ? "Could not load section files." : "სექციის ფაილების ჩატვირთვა ვერ მოხერხდა.");
          setApiFilesData([]);
        }
      } finally {
        setFilesLoading(false);
      }
    };

    loadFiles();

    return () => controller.abort();
  }, [coverPdfNumber, filesData, language, sectionKey]);

  const effectiveFilesData = useMemo(() => {
    if (filesData) {
      return filesData;
    }

    if (apiFilesData !== null) {
      return apiFilesData;
    }

    return createDefaultFilesData({ sectionKey, titleGE, titleEN });
  }, [apiFilesData, filesData, sectionKey, titleGE, titleEN]);

  const availableFilterData = useMemo(() => {
    const usedCategories = new Set();

    effectiveFilesData.forEach((file) => {
      (file.categories || []).forEach((category) => usedCategories.add(category));
    });

    return filterData.filter((filter) => filter.categories.some((category) => usedCategories.has(category)));
  }, [effectiveFilesData, filterData]);

  const initialFilters = useMemo(() => {
    const state = {};
    availableFilterData.forEach((f) => {
      state[f.key] = true;
    });
    return state;
  }, [availableFilterData]);

  const [filters, setFilters] = useState(initialFilters);
  const [openCharts, setOpenCharts] = useState({});
  const [chartType, setChartType] = useState({});
  const [seriesFilters, setSeriesFilters] = useState({});
  const [downloaded, setDownloaded] = useState({});

  useEffect(() => {
    setFilters((prev) => {
      const next = {};

      availableFilterData.forEach((filter) => {
        next[filter.key] = Object.prototype.hasOwnProperty.call(prev, filter.key)
          ? prev[filter.key]
          : true;
      });

      if (Object.keys(next).length === 0) {
        return {};
      }

      if (!Object.values(next).some(Boolean)) {
        next[availableFilterData[0].key] = true;
      }

      return next;
    });
  }, [availableFilterData]);

  const activeCategories = useMemo(() => {
    const categorySet = new Set();
    availableFilterData.forEach((filter) => {
      if (filters[filter.key]) {
        filter.categories.forEach((category) => categorySet.add(category));
      }
    });
    return categorySet;
  }, [availableFilterData, filters]);

  const visibleFiles = useMemo(() => {
    return effectiveFilesData.filter((file) => file.categories.some((category) => activeCategories.has(category)));
  }, [effectiveFilesData, activeCategories]);

  const toggleFilter = (key) => {
    setFilters((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleChart = (file) => {
    const id = file.id;
    const defaultSeriesNames = [
      ...(file.chartdata?.barSeries || []).map((seriesItem) => seriesItem.name),
      ...(file.chartdata?.lineSeries || []).map((seriesItem) => seriesItem.name),
    ];

    setOpenCharts((prev) => ({ ...prev, [id]: !prev[id] }));
    setChartType((prev) => ({ ...prev, [id]: prev[id] || "bar" }));
    setSeriesFilters((prev) => {
      if (prev[id] || defaultSeriesNames.length === 0) {
        return prev;
      }

      return { ...prev, [id]: defaultSeriesNames };
    });
  };

  const setFileChartType = (id, type) => {
    setChartType((prev) => ({ ...prev, [id]: type }));
  };

  const toggleSeriesFilter = (fileId, seriesName) => {
    setSeriesFilters((prev) => {
      const current = prev[fileId] || [];
      const isActive = current.includes(seriesName);
      const next = isActive
        ? current.filter((name) => name !== seriesName)
        : [...current, seriesName];

      return {
        ...prev,
        [fileId]: next,
      };
    });
  };

  const getChartOptions = (file, activeType) => {
    const isLine = activeType === "line";
    const allSeries = isLine
      ? file.chartdata.lineSeries || file.chartdata.barSeries || []
      : file.chartdata.barSeries || [];
    const selectedSeries = seriesFilters[file.id];
    const chartSeries = Array.isArray(selectedSeries) && selectedSeries.length > 0
      ? allSeries.filter((seriesItem) => selectedSeries.includes(seriesItem.name))
      : allSeries;
    const chartTitle = formatChartLabel(language === "EN" ? file.titleEN : file.titleGE);

    return {
      chart: {
        type: isLine ? "line" : "column",
        backgroundColor: "transparent",
        height: 340,
        style: {
          fontFamily: "FiraGO",
        },
      },
      title: {
        text: chartTitle,
        align: "left",
        style: {
          color: "#24324d",
          fontFamily: "FiraGO",
          fontSize: "16px",
          fontWeight: "700",
        },
      },
      credits: { enabled: false },
      legend: { enabled: false },
      xAxis: {
        categories: file.chartdata.labels || [],
        title: { text: null },
        lineColor: "#8ea4c8",
        tickColor: "#8ea4c8",
        labels: {
          style: {
            color: "#37496d",
            fontSize: "12px",
          },
        },
      },
      yAxis: {
        title: { text: null },
        gridLineColor: "#dce5f5",
        labels: {
          style: {
            color: "#37496d",
            fontSize: "12px",
          },
        },
      },
      tooltip: {
        shared: true,
        valueDecimals: 0,
      },
      plotOptions: {
        column: {
          borderRadius: 6,
          pointPadding: 0.12,
          groupPadding: 0.14,
        },
        line: {
          lineWidth: 3,
          marker: {
            enabled: true,
            radius: 4,
          },
        },
        series: {
          animation: false,
        },
      },
      series: chartSeries.map((seriesItem, index) => ({
        type: isLine ? "line" : "column",
        name: formatChartLabel(seriesItem.name),
        data: seriesItem.data,
        color: getSeriesColorByName(file, seriesItem.name),
      })),
    };
  };

  return (
    <div className="w-full max-w-screen-2xl mx-auto px-4 py-10 section-data-page">
      <div className="section-hero">
        <img
          className="section-hero-image"
          src={slideImg}
          alt={language === "EN" ? titleEN : titleGE}
        />
        <button
          className="section-hero-button"
          onClick={() =>
            window.open(
              `/coverpdf/${language === "EN" ? "eng" : "geo"}/${coverPdfNumber}.pdf`,
              "_blank",
            )
          }
        >
          <span className="section-hero-button-label">
            {language === "EN" ? "See more" : "ნახვა"}
          </span>
          <span className="section-hero-button-arrow">
            ›
          </span>
        </button>
      </div>

      <h2
        className="section-page-title font-bold mt-8 mb-4"
        style={{
          color: "#37496d",
          textAlign: "center",
          fontFeatureSettings: '"case" on',
          fontFamily: "FiraGO",
        }}
      >
        {language === "EN" ? titleEN : titleGE}
      </h2>

      <div className="flex justify-center">
        <svg width="70" height="1" viewBox="0 0 70 1" fill="none" xmlns="http://www.w3.org/2000/svg">
          <line y1="0.5" x2="70" y2="0.5" stroke="black" />
        </svg>
      </div>

      <p className="choosecat" style={{ fontFamily: "FiraGO" }}>
        {language === "EN" ? "Data Sources:" : "მონაცემთა წყაროები:"}
      </p>

      <div className="filelist-filter">
        {availableFilterData.map((filter) => (
          <label key={filter.key} className={`filter-item ${filters[filter.key] ? "active" : ""}`}>
            <img src={resolveFilterImage(filter.imageKey, language)} alt="" style={{ width: `${filter.imgWidth}px` }} />
            <p style={{ fontFamily: "FiraGO", fontFeatureSettings: '"case" on' }}>
              {language === "EN" ? filter.labelEN : filter.labelGE}
            </p>
            <input
              type="checkbox"
              checked={filters[filter.key]}
              onChange={() => toggleFilter(filter.key)}
            />
          </label>
        ))}
      </div>

      {filesLoading && (
        <p className="choosecat" style={{ fontFamily: "FiraGO", marginTop: "18px" }}>
          {language === "EN" ? "Loading files..." : "ფაილები იტვირთება..."}
        </p>
      )}

      {filesError && (
        <p className="choosecat" style={{ fontFamily: "FiraGO", marginTop: "18px", color: "#b42318" }}>
          {filesError}
        </p>
      )}

      <div className="filelist">
        {visibleFiles.map((file) => (
          <div key={file.id} className="filelist-item">
            <div className="image">
              <svg width="35" height="33" viewBox="0 0 35 33" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path d="M33.9062 28.4389H18.5937C17.99 28.4389 17.5 27.9489 17.5 27.3451C17.5 26.7414 17.5 6.07393 17.5 5.47018C17.5 4.86643 17.99 4.37643 18.5937 4.37643H33.9062C34.51 4.37643 35 4.86643 35 5.47018V27.3451C35 27.9489 34.51 28.4389 33.9062 28.4389Z" fill="#ECEFF1"></path>
                <path d="M22.9687 10.939H18.5937C17.99 10.939 17.5 10.449 17.5 9.84524C17.5 9.24149 17.99 8.7515 18.5937 8.7515H22.9687C23.5725 8.7515 24.0625 9.24149 24.0625 9.84524C24.0625 10.449 23.5725 10.939 22.9687 10.939Z" fill="#388E3C"></path>
                <path d="M22.9687 15.314H18.5937C17.99 15.314 17.5 14.824 17.5 14.2202C17.5 13.6165 17.99 13.1265 18.5937 13.1265H22.9687C23.5725 13.1265 24.0625 13.6165 24.0625 14.2202C24.0625 14.824 23.5725 15.314 22.9687 15.314Z" fill="#388E3C"></path>
                <path d="M22.9687 19.689H18.5937C17.99 19.689 17.5 19.199 17.5 18.5952C17.5 17.9915 17.99 17.5015 18.5937 17.5015H22.9687C23.5725 17.5015 24.0625 17.9915 24.0625 18.5952C24.0625 19.199 23.5725 19.689 22.9687 19.689Z" fill="#388E3C"></path>
                <path d="M22.9687 24.064H18.5937C17.99 24.064 17.5 23.574 17.5 22.9702C17.5 22.3665 17.99 21.8765 18.5937 21.8765H22.9687C23.5725 21.8765 24.0625 22.3665 24.0625 22.9702C24.0625 23.574 23.5725 24.064 22.9687 24.064Z" fill="#388E3C"></path>
                <path d="M29.5308 10.939H27.3433C26.7395 10.939 26.2495 10.449 26.2495 9.84527C26.2495 9.24153 26.7395 8.75153 27.3433 8.75153H29.5308C30.1345 8.75153 30.6245 9.24153 30.6245 9.84527C30.6245 10.449 30.1345 10.939 29.5308 10.939Z" fill="#388E3C"></path>
                <path d="M29.5308 15.314H27.3433C26.7395 15.314 26.2495 14.824 26.2495 14.2202C26.2495 13.6165 26.7395 13.1265 27.3433 13.1265H29.5308C30.1345 13.1265 30.6245 13.6165 30.6245 14.2202C30.6245 14.824 30.1345 15.314 29.5308 15.314Z" fill="#388E3C"></path>
                <path d="M29.5308 19.689H27.3433C26.7395 19.689 26.2495 19.199 26.2495 18.5952C26.2495 17.9915 26.7395 17.5015 27.3433 17.5015H29.5308C30.1345 17.5015 30.6245 17.9915 30.6245 18.5952C30.6245 19.199 30.1345 19.689 29.5308 19.689Z" fill="#388E3C"></path>
                <path d="M29.5308 24.064H27.3433C26.7395 24.064 26.2495 23.574 26.2495 22.9702C26.2495 22.3665 26.7395 21.8765 27.3433 21.8765H29.5308C30.1345 21.8765 30.6245 22.3665 30.6245 22.9702C30.6245 23.574 30.1345 24.064 29.5308 24.064Z" fill="#388E3C"></path>
                <path d="M19.2915 0.253036C19.0422 0.0452238 18.7075 -0.0444636 18.3925 0.0211613L0.892499 3.30241C0.374062 3.39866 0 3.84928 0 4.37647V28.4389C0 28.9639 0.374062 29.4167 0.892499 29.513L18.3925 32.7942C18.4581 32.8074 18.5259 32.8139 18.5937 32.8139C18.8475 32.8139 19.0947 32.7264 19.2915 32.5624C19.5431 32.3545 19.6875 32.0439 19.6875 31.7202V1.09522C19.6875 0.769285 19.5431 0.460848 19.2915 0.253036Z" fill="#2E7D32"></path>
                <path d="M15.0414 20.063L11.583 16.1102L15.0808 11.6127C15.4526 11.1358 15.3651 10.4489 14.8904 10.0771C14.4158 9.70519 13.7289 9.79269 13.3548 10.2674L10.1151 14.4324L7.38515 11.313C6.98483 10.8536 6.29358 10.8121 5.84296 11.2102C5.38796 11.6083 5.34202 12.2996 5.74015 12.7524L8.74796 16.1911L5.69859 20.1111C5.32671 20.588 5.41421 21.2749 5.8889 21.6467C6.09015 21.802 6.32858 21.8764 6.56265 21.8764C6.88858 21.8764 7.21015 21.732 7.42671 21.4542L10.2158 17.8667L13.3964 21.5002C13.6129 21.7495 13.9148 21.8764 14.2189 21.8764C14.4748 21.8764 14.7308 21.7867 14.9386 21.6052C15.3936 21.207 15.4395 20.5158 15.0414 20.063Z" fill="#FAFAFA"></path>
              </svg>
            </div>
            <div className="title">{language === "EN" ? file.titleEN : file.titleGE}</div>
            {file.chartdata && (
              <a
                href="#"
                className={`show ${openCharts[file.id] ? "open" : ""}`}
                onClick={(e) => {
                  e.preventDefault();
                  toggleChart(file);
                }}
                tabIndex={-1}
              >
                <svg width="50" height="52" viewBox="0 0 50 52" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <rect width="50" height="52" rx="15" fill="#0066E0" />
                  <path d="M33.3962 23H16.6038C16.1001 23 15.8188 23.4415 16.1308 23.743L24.527 31.8265C24.7673 32.0578 25.2301 32.0578 25.473 31.8265L33.8692 23.743C34.1812 23.4415 33.8999 23 33.3962 23Z" fill="white" />
                </svg>
              </a>
            )}
            <a
              className="download"
              href={language === "EN" ? file.hrefEN || file.href : file.hrefGE || file.href}
              target="_blank"
              rel="noreferrer"
              onClick={() => setDownloaded((prev) => ({ ...prev, [file.id]: true }))}
            >
              <div>{language === "EN" ? "Download" : "გადმოწერა"}</div>
              <img
                src={downloadIcon}
                alt=""
                width="28"
                height="28"
                className={`download-icon ${downloaded[file.id] ? "hidden" : ""}`}
              />
              <svg
                width="50"
                height="52"
                viewBox="0 0 50 52"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                className={`downloaded-icon ${downloaded[file.id] ? "visible" : ""}`}
              >
                <rect width="50" height="52" rx="15" fill="#388E3C" />
                <path d="M31.9318 21.3551C31.4588 20.8815 30.6907 20.8818 30.2171 21.3551L22.5 29.0725L19.0701 25.6426C18.5965 25.169 17.8288 25.169 17.3552 25.6426C16.8816 26.1162 16.8816 26.8839 17.3552 27.3575L21.6424 31.6447C21.8791 31.8814 22.1894 32 22.4997 32C22.8101 32 23.1207 31.8817 23.3573 31.6447L31.9318 23.07C32.4054 22.5967 32.4054 21.8287 31.9318 21.3551Z" fill="white" />
              </svg>
            </a>

            {file.chartdata && openCharts[file.id] && (
              <div className="chart" data-id={file.id}>
                <div className="chartdiv1" id={`chartdiv1-${file.id}`}>
                  <div className="chartswitch">
                    <button
                      type="button"
                      className={`chart-toggle-btn ${(chartType[file.id] || "bar") === "bar" ? "active" : ""}`}
                      onClick={() => setFileChartType(file.id, "bar")}
                      aria-label={language === "EN" ? "Show bar chart" : "სვეტოვანი ჩარტის ჩვენება"}
                    >
                      <img src={barchartIcon} alt="" className="chart-toggle-icon" />
                      <span className="chart-toggle-label">{language === "EN" ? "Bar" : "Bar"}</span>
                    </button>
                    <button
                      type="button"
                      className={`chart-toggle-btn ${chartType[file.id] === "line" ? "active" : ""}`}
                      onClick={() => setFileChartType(file.id, "line")}
                      aria-label={language === "EN" ? "Show line chart" : "ხაზოვანი ჩარტის ჩვენება"}
                    >
                      <img src={linechartIcon} alt="" className="chart-toggle-icon" />
                      <span className="chart-toggle-label">{language === "EN" ? "Line" : "Line"}</span>
                    </button>
                  </div>
                  <HighchartsReact
                    highcharts={Highcharts}
                    options={getChartOptions(file, chartType[file.id] || "bar")}
                  />
                  {((file.chartdata.barSeries || []).length > 1 || (file.chartdata.lineSeries || []).length > 1) && (
                    <div className="chartseriesfilters">
                      {getFileSeriesNames(file).map((seriesName) => {
                        const color = getSeriesColorByName(file, seriesName);
                        const isActive = (seriesFilters[file.id] || []).includes(seriesName);
                        return (
                          <button
                            key={seriesName}
                            type="button"
                            className={`series-filter-btn ${isActive ? "active" : ""}`}
                            style={isActive ? { background: color, borderColor: color } : { borderColor: color, color }}
                            onClick={() => toggleSeriesFilter(file.id, seriesName)}
                          >
                            <span className="series-filter-dot" style={{ background: isActive ? "#fff" : color }} />
                            {seriesName}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
                <div className="chartdiv2" id={`chartdiv2-${file.id}`}>
                  <HighchartsReact highcharts={Highcharts} options={getChartOptions(file, "line")} />
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default SectionDataPage;
