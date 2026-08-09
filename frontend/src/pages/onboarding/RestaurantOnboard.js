import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getOnboardingStatus, startOnboarding , getCurrentUser } from "../../services/api";
import OnboardStep1BasicInfo from "./steps/OnboardStep1BasicInfo";
import OnboardStep2Location from "./steps/OnboardStep2Location";
import OnboardStep3Hours from "./steps/OnboardStep3Hours";
import OnboardStep4Documents from "./steps/OnboardStep4Documents";
import OnboardStep5BankDetails from "./steps/OnboardStep5BankDetails";
import OnboardStep6Review from "./steps/OnboardStep6Review";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

const defaultHours = DAYS.map((day) => ({
    dayOfWeek: day,
    isOpen: true,
    openTime: "09:00",
    closeTime: "22:00",
}));

const RestaurantOnboard = () => {
    const navigate = useNavigate();
    const user = getCurrentUser();

    const [onboardingId, setOnboardingId] = useState(null);
    const [currentStep, setCurrentStep] = useState(1);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [basicInfo, setBasicInfo] = useState({
        restaurantName: "", description: "", cuisineTypes: [],
        restaurantType: "BOTH", phone: "", email: "",
    });
    const [location, setLocation] = useState({
        street: "", city: "", state: "", zipCode: "",
        latitude: null, longitude: null, deliveryRadiusKm: 10,
    });
    const [hours, setHours] = useState(defaultHours);
    const [hoursSettings, setHoursSettings] = useState({
        acceptsScheduledOrders: false, slotDurationMinutes: 30,
    });
    const [documents, setDocuments] = useState({
        fssaiLicenseNumber: "", panNumber: "", gstin: "", fssaiFile: null,
    });
    const [bankDetails, setBankDetails] = useState({
        bankAccountHolderName: "", bankAccountNumber: "", bankIfscCode: "", bankName: "",
    });

    useEffect(() => {
        if (!user) { navigate("/login"); return; }
        (async () => {
            try {
                const res = await getOnboardingStatus(user.id);
                const ob = res.data;
                if (ob.status === "APPROVED") { navigate("/partner/dashboard"); return; }
                if (ob.status === "PENDING_REVIEW" || ob.status === "SUBMITTED") {
                    navigate("/partner/onboard/status"); return;
                }
                setOnboardingId(ob.onboardingId);
                setCurrentStep(ob.currentStep || 1);
                populateFromOnboarding(ob);
            } catch {
                try {
                    const res = await startOnboarding(user.id);
                    setOnboardingId(res.data.onboardingId);
                } catch (err) {
                    setError("Failed to start onboarding. Please try again.");
                }
            } finally {
                setLoading(false);
            }
        })();
    }, []); // eslint-disable-line react-hooks/exhaustive-deps

    const populateFromOnboarding = (ob) => {
        if (ob.restaurantName) setBasicInfo({
            restaurantName: ob.restaurantName || "",
            description: ob.description || "",
            cuisineTypes: ob.cuisineTypes ? ob.cuisineTypes.split(",").filter(Boolean) : [],
            restaurantType: ob.restaurantType || "BOTH",
            phone: ob.phone || "",
            email: ob.email || "",
        });
        if (ob.street) setLocation({
            street: ob.street || "", city: ob.city || "",
            state: ob.state || "", zipCode: ob.zipCode || "",
            latitude: ob.latitude || null, longitude: ob.longitude || null,
            deliveryRadiusKm: ob.deliveryRadiusKm || 10,
        });
        if (ob.operatingHours && ob.operatingHours.length === 7) {
            setHours(ob.operatingHours.map((h) => ({
                dayOfWeek: h.dayOfWeek, isOpen: h.isOpen,
                openTime: h.openTime || "09:00", closeTime: h.closeTime || "22:00",
            })));
            setHoursSettings({
                acceptsScheduledOrders: ob.acceptsScheduledOrders || false,
                slotDurationMinutes: ob.slotDurationMinutes || 30,
            });
        }
        if (ob.fssaiLicenseNumber) setDocuments({
            fssaiLicenseNumber: ob.fssaiLicenseNumber || "",
            panNumber: ob.panNumber || "",
            gstin: ob.gstin || "",
            fssaiFile: null,
        });
        if (ob.bankAccountNumber) setBankDetails({
            bankAccountHolderName: ob.bankAccountHolderName || "",
            bankAccountNumber: ob.bankAccountNumber || "",
            bankIfscCode: ob.bankIfscCode || "",
            bankName: ob.bankName || "",
        });
    };

    const handleNext = (newStep) => {
        setCurrentStep(newStep);
        window.scrollTo(0, 0);
    };

    const handleBack = () => {
        setCurrentStep((s) => Math.max(1, s - 1));
        window.scrollTo(0, 0);
    };

    const STEP_LABELS = ["Basic Info", "Location", "Hours", "Documents", "Bank Details", "Review"];

    if (loading) return <div className="text-center mt-5"><div className="spinner-border" /></div>;

    return (
        <div className="row justify-content-center">
            <div className="col-md-8">
                <h3 className="mb-4">Restaurant Partner Onboarding</h3>
                {error && <div className="alert alert-danger">{error}</div>}

                {/* Stepper */}
                <div className="d-flex justify-content-between mb-4">
                    {STEP_LABELS.map((label, idx) => {
                        const step = idx + 1;
                        const done = step < currentStep;
                        const active = step === currentStep;
                        return (
                            <div key={step} className="text-center flex-fill">
                                <div
                                    className={`rounded-circle d-inline-flex align-items-center justify-content-center mb-1 ${
                                        done ? "bg-success text-white" : active ? "bg-primary text-white" : "bg-light border"
                                    }`}
                                    style={{ width: 36, height: 36, fontSize: 14, cursor: done ? "pointer" : "default" }}
                                    onClick={() => done && setCurrentStep(step)}
                                >
                                    {done ? "✓" : step}
                                </div>
                                <div style={{ fontSize: 11, color: active ? "#0d6efd" : "#666" }}>{label}</div>
                            </div>
                        );
                    })}
                </div>

                {/* Step Components */}
                {currentStep === 1 && (
                    <OnboardStep1BasicInfo
                        data={basicInfo} onChange={setBasicInfo}
                        onboardingId={onboardingId} onNext={() => handleNext(2)}
                    />
                )}
                {currentStep === 2 && (
                    <OnboardStep2Location
                        data={location} onChange={setLocation}
                        onboardingId={onboardingId} onNext={() => handleNext(3)} onBack={handleBack}
                    />
                )}
                {currentStep === 3 && (
                    <OnboardStep3Hours
                        hours={hours} onHoursChange={setHours}
                        settings={hoursSettings} onSettingsChange={setHoursSettings}
                        onboardingId={onboardingId} onNext={() => handleNext(4)} onBack={handleBack}
                    />
                )}
                {currentStep === 4 && (
                    <OnboardStep4Documents
                        data={documents} onChange={setDocuments}
                        onboardingId={onboardingId} onNext={() => handleNext(5)} onBack={handleBack}
                    />
                )}
                {currentStep === 5 && (
                    <OnboardStep5BankDetails
                        data={bankDetails} onChange={setBankDetails}
                        onboardingId={onboardingId} onNext={() => handleNext(6)} onBack={handleBack}
                    />
                )}
                {currentStep === 6 && (
                    <OnboardStep6Review
                        basicInfo={basicInfo} location={location}
                        hours={hours} hoursSettings={hoursSettings}
                        documents={documents} bankDetails={bankDetails}
                        onboardingId={onboardingId}
                        onEditStep={setCurrentStep}
                        onSubmitSuccess={() => navigate("/partner/onboard/status")}
                    />
                )}
            </div>
        </div>
    );
};

export default RestaurantOnboard;
