package campus

import campus.activities.processDueBookings
import campus.db.Tx
import campus.teacher.expireOverdueReviews

fun Tx.processDue() {
    processDueBookings()
    expireOverdueReviews()
}
