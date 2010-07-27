GENERATING BUTTON PNGS

This directory includes alternate button icons for nookLauncher in SVG format;
they have been created and should be edited using Inkscape,
<http://inkscape.org/>.

Each of the SVGs contains a complete set of buttons in identical order.  The
regular look is on the "normal" layer; the "pressed" looks is achieved by
overlaying it with the "pressed" layer.  Make sure that both layers are
visible before proceeding after editing an SVG.

Button PNGs can be created using the "buttons2png.pl" script also located in
this directory (for its requirements, see the comment at the start of its
code).  This script can on the one hand create PNGs and put them in specific
destination paths, or on the other hand put them all in a user-specified
destination directory.  In order to do the former, first adapt the paths in
$::NOOKDEVS, $::NOOKAPPS, and $::MAPPING accordingly, if necessary.  For the
latter, simply run with the "--dir" option (see the help output when run with
"--help").

By default, the script will convert the icons symlinked to "buttons.svg" in
its directory; to use a different set, either change the symlink or specify
the alternate file using the "--file" option.

