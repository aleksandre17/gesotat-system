# Control Plane UI composition boundary

`platform/apps/geostat/frontend/geostat-system-app` is product source. Its
deployment composition belongs to this service boundary. Source-local Compose,
Nginx and deploy copies are transitional until migration acceptance; no new
deployment artifact may be added to the product source.
