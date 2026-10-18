import { Button, Stack, Typography } from "@mui/material";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <Stack alignItems="center" spacing={2} sx={{ mt: 12 }}>
      <Typography variant="h4">Page not found</Typography>
      <Button component={Link} to="/" variant="contained">
        Return to DevPilot
      </Button>
    </Stack>
  );
}
