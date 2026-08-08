import React, { useState, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icon
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
    iconUrl: require('leaflet/dist/images/marker-icon.png'),
    shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const LocationMarker = ({ position, setPosition }) => {
    const map = useMapEvents({
        click(e) {
            setPosition(e.latlng);
            map.flyTo(e.latlng, map.getZoom());
        },
    });

    useEffect(() => {
        if (position) {
            map.flyTo(position, map.getZoom());
        }
    }, [position, map]);

    return position === null ? null : (
        <Marker position={position}></Marker>
    );
};

// Component to handle "Locate Me" functionality
const LocateControl = ({ setPosition }) => {
    const map = useMap();

    const handleLocateMe = () => {
        map.locate().on("locationfound", function (e) {
            setPosition(e.latlng);
            map.flyTo(e.latlng, map.getZoom());
        });
    };

    return (
        <div style={{ position: 'absolute', top: '10px', right: '10px', zIndex: 1000 }}>
            <button type="button" className="btn btn-sm btn-light border shadow-sm" onClick={handleLocateMe}>
                📍 Locate Me
            </button>
        </div>
    );
};

const AddressMap = ({ initialLat, initialLng, onLocationSelect }) => {
    const [position, setPosition] = useState(initialLat && initialLng ? { lat: initialLat, lng: initialLng } : null);

    useEffect(() => {
        if (initialLat && initialLng) {
            setPosition({ lat: initialLat, lng: initialLng });
        }
    }, [initialLat, initialLng]);

    useEffect(() => {
        if (position) {
            onLocationSelect(position.lat, position.lng);
        }
    }, [position, onLocationSelect]);

    return (
        <div style={{ height: '350px', width: '100%', marginBottom: '1rem', position: 'relative' }}>
            <MapContainer
                center={initialLat && initialLng ? [initialLat, initialLng] : [51.505, -0.09]}
                zoom={13}
                style={{ height: '100%', width: '100%', zIndex: 0 }}
            >
                <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                />
                <LocationMarker position={position} setPosition={setPosition} />
                <LocateControl setPosition={setPosition} />
            </MapContainer>
        </div>
    );
};

export default AddressMap;
