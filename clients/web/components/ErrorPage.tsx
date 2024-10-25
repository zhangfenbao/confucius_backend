import { AlertCircle } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "./ui/alert";

interface ErrorPageProps {
  title: string;
  description: string;
}

export default function ErrorPage({ title, description }: ErrorPageProps) {
  return (
    <div className="flex items-center justify-center h-svh max-w-lg mx-auto animate-appear">
      <Alert variant="destructive" className="shadow-long">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle>{title}</AlertTitle>
        <AlertDescription>{description}</AlertDescription>
      </Alert>
    </div>
  );
}
