const UserProfile = require('../models/UserProfile');

/**
 * Syncs user profile upon authentication
 */
exports.syncUserProfile = async (req, res, next) => {
  try {
    const { uid, email, name } = req.user;
    const { displayName } = req.body;

    let user = await UserProfile.findOne({ firebaseUid: uid });

    if (!user) {
      user = await UserProfile.create({
        firebaseUid: uid,
        email: email,
        displayName: displayName || name || 'Customer',
        role: req.user.admin ? 'admin' : 'user'
      });
    } else {
      user.email = email;
      if (displayName) user.displayName = displayName;
      if (req.user.admin && user.role !== 'admin') {
        user.role = 'admin';
      }
      await user.save();
    }

    res.json({
      success: true,
      data: {
        uid: user.firebaseUid,
        email: user.email,
        displayName: user.displayName,
        role: user.role,
        isAdmin: req.user.admin === true
      }
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Returns currently authenticated user details
 */
exports.getCurrentUser = async (req, res, next) => {
  try {
    const { uid, email, name, admin: isAdmin } = req.user;

    const profile = await UserProfile.findOne({ firebaseUid: uid });

    res.json({
      success: true,
      data: {
        uid,
        email,
        displayName: profile ? profile.displayName : name,
        role: profile ? profile.role : (isAdmin ? 'admin' : 'user'),
        isAdmin: isAdmin === true
      }
    });
  } catch (err) {
    next(err);
  }
};
