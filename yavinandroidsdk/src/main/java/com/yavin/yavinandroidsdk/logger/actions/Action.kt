package com.yavin.yavinandroidsdk.logger.actions

import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat

sealed class Action(internal val description: String) {

    abstract fun describeAction(): String

    open class SimpleAction(description: String) : Action(description) {
        override fun describeAction(): String {
            return "Action: $description"
        }
    }

    open class StringValueAction(protected val currentValue: String, description: String) : SimpleAction(description) {
        override fun describeAction(): String {
            return "Action: $description.".apply {
                plus("\n\tValue: $currentValue")
            }
        }
    }

    open class BooleanValueAction(protected val currentValue: Boolean, description: String) : SimpleAction(description) {
        override fun describeAction(): String {
            return "Action: $description.".apply {
                plus("\n\tValue: $currentValue")
            }
        }
    }

    class ButtonClicked(view: Button, private val details: String = "") : SimpleAction(view.text.toString()) {
        override fun describeAction(): String {
            return "Action: Button with text \"$description\" clicked.".apply {
                if (details.isNotEmpty()) {
                    this.plus("\n\tDetails: $details")
                }
            }
        }
    }

    class ImageButtonClicked(view: ImageButton, private val details: String = "") : SimpleAction(view.contentDescription.toString()) {
        override fun describeAction(): String {
            return "Action: ImageButton with contentDescription \"$description\" clicked.".apply {
                if (details.isNotEmpty()) {
                    this.plus("\n\tDetails: $details")
                }
            }
        }
    }

    class SwitchChanged(
        view: SwitchCompat,
        private val details: String = ""
    ) : BooleanValueAction(view.isEnabled, view.text.toString()) {
        override fun describeAction(): String {
            return "Action: Switch with text \"$description\" changed.".apply {
                this.plus("\n\tOld value: ${!currentValue}")
                this.plus("\n\tNew value: $currentValue")
                if (details.isNotEmpty()) {
                    this.plus("\n\tDetails: $details")
                }
            }
        }
    }

    class SpinnerChanged(
        view: Spinner,
        private val details: String = ""
    ) : StringValueAction(view.selectedItem.toString(), view.contentDescription.toString()) {
        override fun describeAction(): String {
            return "Action: Spinner with contentDescription \"$description\" changed.".apply {
                this.plus("\n\tNew value: $currentValue")
                if (details.isNotEmpty()) {
                    this.plus("\n\tDetails: $details")
                }
            }
        }
    }

    override fun toString(): String {
        return this::class.java.simpleName
    }
}