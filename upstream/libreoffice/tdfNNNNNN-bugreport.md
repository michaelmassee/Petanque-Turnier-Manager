# Bugzilla-Entwurf: UNO-API liefert keine Hintergrundfarbe aus bedingter Formatierung

Einreichen unter https://bugs.documentfoundation.org/enter_bug.cgi?product=LibreOffice
Vorher nach Duplikaten suchen (Stichworte: `CellBackColor conditional`, `GetCondResult UNO`).
Danach `NNNNNN` in Patch-Dateiname und Commit-Message durch die Bugnummer ersetzen.

| Feld | Wert |
|---|---|
| Product | LibreOffice |
| Component | Calc |
| Version | Master old -3 / aktuelle Release-Version |
| Severity | enhancement |
| Keywords | (leer lassen – QA setzt sie) |

---

**Summary:**
UNO API: no way to read the background colour a cell is displayed with when conditional formatting applies

**Description:**
`CellBackColor` (com.sun.star.table.CellProperties) only reflects direct formatting and the cell
style. When a conditional format (a condition with a cell style, or a colour scale) gives a cell a
different background, the API still returns the unconditional value (usually -1 / transparent).
There is no other property or interface to obtain the colour actually displayed.

This affects extensions that render sheet content themselves, e.g. an own HTML/web export: every
conditionally formatted background is lost. The built-in HTML filter does not have this problem
because it evaluates the conditions directly at document level.

**Steps to Reproduce:**
1. New Calc document, enter `10` in A1.
2. Format > Conditional > Condition: "Cell value is greater than 5", style "Accent 3" (or any style
   with a background colour). A1 is now displayed with that background.
3. Run this Basic macro:
   ```
   Sub Main
       Dim oCell : oCell = ThisComponent.Sheets(0).getCellByPosition(0, 0)
       MsgBox oCell.CellBackColor
   End Sub
   ```

**Actual Results:**
`-1` (transparent), although the cell is displayed with a coloured background.
The same happens for a colour scale.

**Expected Results:**
A way to read the effective (displayed) background colour through UNO.

**Analysis:**
- `ScCellRangesBase::GetOnePropertyValue()` → `GetCurrentDataSet()` → `GetCurrentAttrsDeep()` →
  `ScDocument::CreateSelectionPattern()` (sc/source/ui/unoobj/cellsuno.cxx) only uses the cell's
  `ScPatternAttr`. `ScDocument::GetCondResult()` is not called anywhere in sc/source/ui/unoobj/.
- The HTML filter does evaluate it: `ScHTMLExport::WriteCell()` (sc/source/filter/html/htmlexp.cxx)
  calls `GetCondResult()` and reads `ATTR_BACKGROUND` with the condition item set, plus the colour
  scale colour.

**Proposed solution (patch available):**
Leave `CellBackColor` unchanged (it is read/write and describes the set formatting; changing its
meaning would make code that copies properties between cells bake conditional colours into direct
formatting). Add a new read-only property `DisplayedCellBackColor` (long) to the `SheetCell`
service, similar to `FormulaResultType2` / `CellContentType`. It uses the same precedence as
`ScDocument::FillInfo()` for painting: colour scale colour, then the background of the cell style
applied by a matching condition, then the cell's own background.

Patch: will be submitted to Gerrit and linked here.

**Additional Info:**
Found while developing the Petanque-Turnier-Manager Calc extension, which renders sheets as HTML
for a built-in web server.
