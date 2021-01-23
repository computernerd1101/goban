@file:Suppress("unused", "ClassName")

package com.computernerd1101.goban.desktop.resources

import java.util.*

fun gobanDesktopResources(locale: Locale = Locale.getDefault(Locale.Category.DISPLAY)): ResourceBundle {
    return ResourceBundle.getBundle(
        "com.computernerd1101.goban.desktop.resources.GobanDesktopResources",
        locale
    )
}

fun gobanDesktopFormatResources(locale: Locale = Locale.getDefault(Locale.Category.FORMAT)): ResourceBundle {
    return ResourceBundle.getBundle(
        "com.computernerd1101.goban.desktop.resources.GobanDesktopFormatResources",
        locale
    )
}

class GobanDesktopResources: ListResourceBundle() {

    override fun getContents(): Array<out Array<out Any>> {
        return arrayOf(
            arrayOf("Confirm.Message", "Are you sure?"),

            arrayOf("Default", "Default"),
            arrayOf("NewGame", "New Game"),
            arrayOf("RulesPreset.JAPANESE", "Japanese Rules"),
            arrayOf("RulesPreset.AGA", "AGA Rules"),
            arrayOf("RulesPreset.NZ", "New Zealand Rules"),
            arrayOf("RulesPreset.GOE", "Ing Rules"),
            arrayOf("RulesPreset.CUSTOM", "Custom Rules..."),
            arrayOf("ScoreType.AREA", "Score by Area"),
            arrayOf("ScoreType.TERRITORY", "Score by Territory"),
            arrayOf("TimeLimit.Prompt", "Time Limit: "),
            arrayOf("Overtime.Header", "Overtime..."),
            arrayOf("Overtime.Prefix", "Overtime: "),
            arrayOf("Overtime.Suffix", ""),
            arrayOf("PropertyTranslator.Prefix", ""),
            arrayOf("PropertyTranslator.Suffix", ": "),
            arrayOf("HandicapType.FIXED", "Fixed Handicap: "),
            arrayOf("HandicapType.FREE", "Free Handicap: "),
            arrayOf("SizeHeader.SIZE", "Size: "),
            arrayOf("SizeHeader.WIDTH", "Width: "),
            arrayOf("SizeHeader.HEIGHT", "Height: "),
            arrayOf("AllowSuicide", "Allow Suicide?"),

            arrayOf("Up", "Up"),
            arrayOf("Down", "Down"),
            arrayOf("Create", "Create"),
            arrayOf("Delete", "Delete"),
            arrayOf("Copy", "Copy"),
            arrayOf("Paste", "Paste"),
            arrayOf("Previous", "Previous"),
            arrayOf("Next", "Next"),
            arrayOf("Add", "Add"),
            arrayOf("Remove", "Remove"),

            arrayOf("Comment", "Comment"),

            arrayOf("Move", "Move"),
            arrayOf("Move.Forced", "Forced?"),
            arrayOf("Move.Annotation.Header", "Move Annotation..."),
            arrayOf("Move.Number.Prompt", "Move Number: "),
            arrayOf("Time.Prompt", "Time: "),
            arrayOf("Overtime.Prompt", "Overtime: "),

            arrayOf("Setup", "Setup"),
            arrayOf("Setup.Cancel", "Cancel Setup?"),
            arrayOf("Setup.NextPlayer.Prompt", "Next Player:"),

            arrayOf("Node", "Node"),
            arrayOf("Node.Delete", "Delete node"),
            arrayOf("Node.Name.Prompt", "Node name: "),
            arrayOf("Node.Value.Prompt", "Node value: "),
            arrayOf("Node.PositionState.Header", "Position state..."),
            arrayOf(
                "Node.Hotspot.Values",
                arrayOf("No hotspot", "Hotspot", "Major hotspot")
            ),
            arrayOf("PrintMethod.Default.Prefix", ""),
            arrayOf("PrintMethod.Default.Suffix", " (Default)"),
            arrayOf(
                "Figure.Mode",
                arrayOf("No figure", "New figure", "New figure settings...")
            ),
            arrayOf("Figure.Name.Prompt", "Diagram: "),
            arrayOf("Figure.Default", "Default figure settings"),
            arrayOf("Figure.Coordinates.Hide", "Hide coordinates"),
            arrayOf("Figure.Name.Hide", "Hide figure name"),
            arrayOf("Figure.Moves.Unshown", "Ignore unshown moves"),
            arrayOf("Figure.Captured.Show", "Show captured stones"),
            arrayOf("Figure.Hoshi.Hide", "Hide hoshi dots"),

            arrayOf("GameInfo", "Game Info"),
            arrayOf("GameInfo.Node", "Game info node"),
            arrayOf("GameInfo.Warning.Children", "Selected node has children with game info set. Overwrite?"),
            arrayOf("GameInfo.Warning.Parent", "Selected node has a parent with game info set. Overwrite?"),
            arrayOf("GameInfo.Warning.Selected", "Selected node has game info set. Overwrite?"),
            arrayOf("GameInfo.Warning.Title", "Overwrite game info?"),
            arrayOf("GameInfo.Delete.Confirm", "Are you sure you want to delete the game info from this node?"),
            arrayOf("GameInfo.Delete.Title", "Delete game info?"),
            arrayOf("GameInfo.Player.Prompt", "Player: "),
            arrayOf("GameInfo.Team.Prompt", "Team: "),
            arrayOf("GameInfo.Rank.Prompt", "Rank: "),
            arrayOf("GameInfo.Handicap.Prompt", "Handicap: "),
            arrayOf("GameInfo.Komi.Prompt", "Komi: "),
            arrayOf("GameInfo.Result.Header", "Result..."),
            arrayOf("GameInfo.Result.Winner.Black", "Black wins..."),
            arrayOf("GameInfo.Result.Winner.White", "White wins..."),
            arrayOf("GameInfo.Result.Winner.Forfeit", "by default"),
            arrayOf("GameInfo.Result.Winner.Resign", "by resignation"),
            arrayOf("GameInfo.Result.Winner.Time", "by time"),
            arrayOf("GameInfo.Result.Winner.Amount", "by amount: "),
            arrayOf("GameInfo.Result.Unknown", "Unknown result"),
            arrayOf("GameInfo.Result.Void", "Void result"),
            arrayOf("GameInfo.Result.Draw", "Draw"),

            arrayOf("GameInfo.Name.Prompt", "Game name: "),
            arrayOf("GameInfo.Source.Prompt", "Game source: "),
            arrayOf("GameInfo.User.Prompt", "Game user: "),
            arrayOf("GameInfo.Copyright.Prompt", "Copyright: "),
            arrayOf("GameInfo.Location.Prompt", "Game location: "),
            arrayOf("GameInfo.Event.Name.Prompt", "Event name: "),
            arrayOf("GameInfo.Round.Type.Prompt", "Round type: "),
            arrayOf("GameInfo.Annotation.Provider.Prompt", "Annotation provider: "),
            arrayOf("GameInfo.Rules.Prompt", "Rules: "),
            arrayOf("GameInfo.Opening.Type.Prompt", "Opening type: "),
            arrayOf("GameInfo.Comment.Prompt", "Game Comment:"),
            arrayOf("Date.Year", "Year"),
            arrayOf("Date.Month", "Month"),
            arrayOf("Date.Day", "Day"),
            arrayOf("Date.Today", "Today"),

            arrayOf("Root", "Root"),
            arrayOf("Root.Markup.Auto", "Auto-markup"),
            arrayOf("Root.Variations.Prompt", "Show variations of:"),
            arrayOf("Root.Variations.Children", "Child nodes"),
            arrayOf("Root.Variations.Siblings", "Sibling nodes"),
            arrayOf("Encoding.Prompt", "Encoding: "),
            arrayOf("Encoding.Default", "System default"),

            arrayOf("ToolBar.B", "Black to play"),
            arrayOf("ToolBar.W", "White to play"),
            arrayOf("ToolBar.LB", "Label markup"),
            arrayOf("ToolBar.SL", "Select markup"),
            arrayOf("ToolBar.MA", "X markup"),
            arrayOf("ToolBar.TR", "Triangle markup"),
            arrayOf("ToolBar.CR", "Circle markup"),
            arrayOf("ToolBar.SQ", "Square markup"),
            arrayOf("ToolBar.X", "Delete point markup"),
            arrayOf("ToolBar.LN", "Line markup"),
            arrayOf("ToolBar.AR", "Arrow markup"),
            arrayOf("ToolBar.XLN", "Delete line markup"),
            arrayOf("ToolBar.DD", "Dim part of board"),
            arrayOf("ToolBar.XDD", "Un-dim entire board"),
            arrayOf("ToolBar.DD.Inherit", "Inherit dim part of board"),
            arrayOf("ToolBar.VW", "Visible part of board"),
            arrayOf("ToolBar.XVW", "Reset visibility of entire board"),
            arrayOf("ToolBar.VW.Inherit", "Inherit visible part of board"),

            arrayOf("Markup.Label.Prompt", "Label:"),
            arrayOf("Score.Black.Prefix", "Black score: "),
            arrayOf("Score.Black.Suffix", ""),
            arrayOf("Score.White.Prefix", "White score: "),
            arrayOf("Score.White.Suffix", ""),

        )
    }

}
