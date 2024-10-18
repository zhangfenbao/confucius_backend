"use client";

import emitter from "@/lib/eventEmitter";
import { Menu, Settings2 } from "lucide-react";
import React from "react";

interface NavbarProps {}

const Navbar: React.FC<NavbarProps> = () => {
  const handleSidebarToggle = () => {
    emitter.emit("toggleSidebar");
  };

  const handleSettingsToggle = () => {
    emitter.emit("toggleSettings");
  };

  return (
    <div className="bg-background flex items-center justify-between p-4 sticky top-0 z-10">
      {/* Sidebar Toggle Button */}
      <button
        className="p-2 rounded-md hover:bg-secondary focus:outline-none lg:hidden"
        onClick={handleSidebarToggle}
      >
        <Menu className="w-6 h-6" />
      </button>

      {/* Settings Icon */}
      <button
        className="p-2 rounded-md hover:bg-secondary focus:outline-none ms-auto"
        onClick={handleSettingsToggle}
      >
        <Settings2 className="w-6 h-6" />
      </button>
    </div>
  );
};

export default Navbar;
