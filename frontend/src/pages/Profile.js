import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getUserProfile, updateUserProfile } from '../services/api';
import './Profile.css';

function Profile() {
    const [user, setUser] = useState({ name: '', email: '', address: '' });
    const [editing, setEditing] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const storedUser = JSON.parse(localStorage.getItem('user'));
        if (!storedUser) {
            navigate('/login');
            return;
        }

        getUserProfile(storedUser.id)
            .then(response => setUser(response.data))
            .catch(error => console.error('Error fetching profile:', error));
    }, [navigate]);

    const handleSave = () => {
        updateUserProfile(user.id, user)
            .then(response => {
                setUser(response.data);
                localStorage.setItem('user', JSON.stringify(response.data));
                setEditing(false);
                alert('Profile updated successfully!');
            })
            .catch(error => console.error('Error updating profile:', error));
    };

    return (
        <div className="profile-container">
            <h2>My Profile</h2>

            <div className="profile-card">
                <div className="profile-header">
                    <div className="profile-avatar">{user.name?.charAt(0) || 'U'}</div>
                    <button onClick={() => setEditing(!editing)} className="edit-btn">
                        {editing ? 'Cancel' : 'Edit Profile'}
                    </button>
                </div>

                <div className="profile-form">
                    <div className="form-group">
                        <label>Name</label>
                        {editing ? (
                            <input
                                type="text"
                                value={user.name || ''}
                                onChange={(e) => setUser({ ...user, name: e.target.value })}
                            />
                        ) : (
                            <p>{user.name}</p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Email</label>
                        {editing ? (
                            <input
                                type="email"
                                value={user.email || ''}
                                onChange={(e) => setUser({ ...user, email: e.target.value })}
                            />
                        ) : (
                            <p>{user.email}</p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Address</label>
                        {editing ? (
                            <textarea
                                value={user.address || ''}
                                onChange={(e) => setUser({ ...user, address: e.target.value })}
                            />
                        ) : (
                            <p>{user.address || 'No address set'}</p>
                        )}
                    </div>

                    {editing && (
                        <button onClick={handleSave} className="save-btn">Save Changes</button>
                    )}
                </div>

                <div className="profile-actions">
                    <button onClick={() => navigate('/orders')} className="action-btn">
                        📦 View Order History
                    </button>
                    <button onClick={() => {
                        localStorage.removeItem('user');
                        navigate('/');
                    }} className="action-btn logout">
                        🚪 Logout
                    </button>
                </div>
            </div>
        </div>
    );
}

export default Profile;
