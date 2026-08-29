                    )
                }
            } // end of LazyColumn
                } // end of else
            } // end of Crossfade
        } // end of Column
    } // end of Scaffold
} // end of SettingUi

/** True when [query] is blank (everything passes) or any [haystacks] entry contains it. */
private fun matchesQuery(query: String, haystacks: List<String>): Boolean =
    query.isBlank() || haystacks.any { it.contains(query, ignoreCase = true) }

/**
 * Renders [content] only when the search [query] is blank or matches [label] / any of the
 * extra space-joined [keywords]. Lets individual rows hide while their section stays
 * visible (section-level gates decide whether the section appears at all).
 */
@Composable
private fun SearchableItem(
