package com.orbis.orbis.listeners

import com.orbis.orbis.models.group.GroupDetails

interface SearchClick {
    public fun onGroupSearchClick(groupDetails: GroupDetails)
}