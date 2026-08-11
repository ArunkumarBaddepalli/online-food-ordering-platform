import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { verifyEmail } from "../services/api";

/** Lands here from the link in the confirmation email. */
const VerifyEmail = () => {
    const [params] = useSearchParams();
    const [state, setState] = useState("checking");
    const [message, setMessage] = useState("");

    useEffect(() => {
        const token = params.get("token");
        if (!token) {
            setState("failed");
            setMessage("That link is missing its code.");
            return;
        }

        verifyEmail(token)
            .then(() => {
                setState("done");
                // The stored session predates verification, so refresh the flag
                // rather than making the user sign in again.
                try {
                    const user = JSON.parse(localStorage.getItem("user"));
                    if (user) {
                        localStorage.setItem("user", JSON.stringify({ ...user, emailVerified: true }));
                    }
                } catch {
                    // A malformed entry is not worth failing a confirmation over.
                }
            })
            .catch((e) => {
                setState("failed");
                setMessage(e.response?.data || "That link is not valid or has expired.");
            });
    }, [params]);

    if (state === "checking") {
        return <div className="text-center mt-5"><div className="spinner-border" /></div>;
    }

    return (
        <div className="text-center mt-5">
            <div className="fs-1 mb-2">{state === "done" ? "✅" : "⚠️"}</div>
            <h4>{state === "done" ? "Your email is confirmed" : "We could not confirm that"}</h4>
            <p className="text-muted">
                {state === "done"
                    ? "Thanks. You can order now, and reset your password if you ever need to."
                    : message}
            </p>
            <Link className="btn btn-success" to="/">Start ordering</Link>
        </div>
    );
};

export default VerifyEmail;
